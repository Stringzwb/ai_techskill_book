package com.aitechskill.book.share.controller;

import com.aitechskill.book.community.domain.response.CommunityPostResponse;
import com.aitechskill.book.community.service.CommunityService;
import com.aitechskill.book.document.domain.response.DocumentDetailResponse;
import com.aitechskill.book.document.service.KnowledgeDocumentService;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.HtmlUtils;

/**
 * 为微信等链接预览服务端渲染分享元信息，页面访问后再进入对应的 SPA 内容页。
 */
@Controller
@RequestMapping("/share")
public class SharePageController {

    private final KnowledgeDocumentService documentService;
    private final CommunityService communityService;
    private final String publicBaseUrl;

    public SharePageController(
            KnowledgeDocumentService documentService,
            CommunityService communityService,
            @Value("${app.share.public-base-url}") String publicBaseUrl) {
        this.documentService = documentService;
        this.communityService = communityService;
        this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
    }

    @GetMapping(value = "/documents/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> document(@PathVariable long id) {
        DocumentDetailResponse document = documentService.getPublishedDocument(id);
        String destination = "/documents/" + document.id();
        return sharePage(document.title(), document.summary(), "技术文档", "/share/documents/" + document.id(), destination);
    }

    @GetMapping(value = "/posts/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> post(@PathVariable long id) {
        CommunityPostResponse post = communityService.get(id, 0L);
        return sharePage(post.title(), postDescription(post), "分享库", "/share/posts/" + post.id(),
                "/library?post=" + post.id());
    }

    /** 固定的 1200 x 630 PNG 分享封面，满足微信和常见社交平台的预览尺寸。 */
    @GetMapping(value = "/cover.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> cover() throws IOException {
        BufferedImage image = new BufferedImage(1200, 630, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(15, 31, 28));
            graphics.fillRect(0, 0, 1200, 630);
            graphics.setColor(new Color(26, 119, 104));
            graphics.fillRoundRect(76, 82, 86, 86, 18, 18);
            graphics.setColor(new Color(216, 244, 237));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 33));
            graphics.drawString("AI", 99, 138);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 55));
            graphics.drawString("技术岗 AI 知识库", 76, 286);
            graphics.setColor(new Color(166, 194, 186));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 27));
            graphics.drawString("结构化知识 · 工程化实践 · 持续成长", 79, 340);
            graphics.setColor(new Color(43, 144, 125));
            graphics.fillRect(78, 405, 540, 5);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePublic())
                .body(output.toByteArray());
    }

    private ResponseEntity<String> sharePage(
            String title, String description, String section, String canonicalPath, String destination) {
        String absoluteUrl = publicBaseUrl + canonicalPath;
        String targetUrl = publicBaseUrl + destination;
        String html = """
                <!doctype html><html lang="zh-CN"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
                <title>%s</title><meta name="description" content="%s">
                <meta property="og:type" content="article"><meta property="og:title" content="%s">
                <meta property="og:description" content="%s"><meta property="og:url" content="%s">
                <meta property="og:site_name" content="技术岗 AI 知识库"><meta property="og:image" content="%s/share/cover.png">
                <meta name="twitter:card" content="summary_large_image">
                <meta http-equiv="refresh" content="0;url=%s"></head>
                <body><main><small>%s</small><h1>%s</h1><p>%s</p><a href="%s">打开内容</a></main>
                <script>location.replace(%s)</script></body></html>
                """.formatted(
                escape(title), escape(description), escape(title), escape(description), escape(absoluteUrl),
                escape(publicBaseUrl), escape(targetUrl), escape(section), escape(title), escape(description),
                escape(targetUrl), javascriptString(targetUrl));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Robots-Tag", "noindex")
                .body(html);
    }

    private static String postDescription(CommunityPostResponse post) {
        String value = post.markdown();
        if (value == null || value.isBlank()) {
            value = post.vote() != null ? post.vote().question() : post.linkDomain();
        }
        if (value == null || value.isBlank()) {
            value = "来自技术岗 AI 知识库的工程实践分享。";
        }
        String plain = value.replaceAll("[#>*_`]+", " ").replaceAll("\\s+", " ").trim();
        return plain.length() > 110 ? plain.substring(0, 110) + "…" : plain;
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private static String javascriptString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
