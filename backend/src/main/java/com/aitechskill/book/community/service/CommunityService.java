package com.aitechskill.book.community.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.community.domain.entity.CommunityAttachmentEntity;
import com.aitechskill.book.community.domain.entity.CommunityCommentEntity;
import com.aitechskill.book.community.domain.entity.CommunityPostEntity;
import com.aitechskill.book.community.domain.entity.CommunityPostTagEntity;
import com.aitechskill.book.community.domain.entity.CommunityVoteEntity;
import com.aitechskill.book.community.domain.entity.CommunityVoteOptionEntity;
import com.aitechskill.book.community.domain.request.CommunityCommentCreateRequest;
import com.aitechskill.book.community.domain.request.CommunityPostCreateRequest;
import com.aitechskill.book.community.domain.request.CommunityVoteRequest;
import com.aitechskill.book.community.domain.response.CommunityAttachmentContent;
import com.aitechskill.book.community.domain.response.CommunityAttachmentPreviewResponse;
import com.aitechskill.book.community.domain.response.CommunityCommentResponse;
import com.aitechskill.book.community.domain.response.CommunityPostPageResponse;
import com.aitechskill.book.community.domain.response.CommunityPostResponse;
import com.aitechskill.book.community.mapper.CommunityAttachmentMapper;
import com.aitechskill.book.community.mapper.CommunityCommentMapper;
import com.aitechskill.book.community.mapper.CommunityPostMapper;
import com.aitechskill.book.community.mapper.CommunityPostTagMapper;
import com.aitechskill.book.community.mapper.CommunityVoteMapper;
import com.aitechskill.book.community.mapper.CommunityVoteOptionMapper;
import com.aitechskill.book.community.mapper.CommunityVoteRecord;
import com.aitechskill.book.storage.domain.StorageObjectStreamRequest;
import com.aitechskill.book.storage.domain.StoredObjectContent;
import com.aitechskill.book.storage.domain.StoredObjectStream;
import com.aitechskill.book.storage.service.ObjectStorageService;
import com.aitechskill.book.tag.domain.entity.KnowledgeTagEntity;
import com.aitechskill.book.tag.mapper.KnowledgeTagMapper;
import com.aitechskill.book.user.domain.entity.UserEntity;
import com.aitechskill.book.user.domain.enums.UserRole;
import com.aitechskill.book.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CommunityService {
    private static final Set<String> TYPES = Set.of("QUESTION", "IMAGE", "LINK", "FILE", "VOTE");
    private static final Set<String> IMAGES = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> FILES = Set.of("txt", "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "md", "markdown");
    private static final Set<String> TEXT_PREVIEWS = Set.of("txt", "md", "markdown", "doc", "docx", "ppt", "pptx", "xls", "xlsx");
    private static final int PREVIEW_CHARACTER_LIMIT = 120_000;

    private final CommunityPostMapper posts;
    private final CommunityPostTagMapper postTags;
    private final CommunityAttachmentMapper attachments;
    private final CommunityCommentMapper comments;
    private final CommunityVoteMapper votes;
    private final CommunityVoteOptionMapper options;
    private final KnowledgeTagMapper tags;
    private final UserMapper users;
    private final ObjectStorageService storage;
    private final CommunityLinkComplianceService links;
    private final long attachmentMax;
    private final long imageMax;
    private final long previewMax;

    public CommunityService(
            CommunityPostMapper posts,
            CommunityPostTagMapper postTags,
            CommunityAttachmentMapper attachments,
            CommunityCommentMapper comments,
            CommunityVoteMapper votes,
            CommunityVoteOptionMapper options,
            KnowledgeTagMapper tags,
            UserMapper users,
            ObjectStorageService storage,
            CommunityLinkComplianceService links,
            @Value("${app.community.attachment-max-size}") DataSize attachmentMax,
            @Value("${app.community.image-max-size}") DataSize imageMax,
            @Value("${app.community.preview-max-size}") DataSize previewMax) {
        this.posts = posts;
        this.postTags = postTags;
        this.attachments = attachments;
        this.comments = comments;
        this.votes = votes;
        this.options = options;
        this.tags = tags;
        this.users = users;
        this.storage = storage;
        this.links = links;
        this.attachmentMax = attachmentMax.toBytes();
        this.imageMax = imageMax.toBytes();
        this.previewMax = previewMax.toBytes();
    }

    @Transactional(readOnly = true)
    public CommunityPostPageResponse list(String keyword, Long tagId, String postType, int page, int size, long viewerId) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(30, Math.max(1, size));
        LambdaQueryWrapper<CommunityPostEntity> query = Wrappers.lambdaQuery(CommunityPostEntity.class)
                .eq(CommunityPostEntity::getDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            query.and(q -> q.like(CommunityPostEntity::getTitle, keyword.trim())
                    .or()
                    .like(CommunityPostEntity::getMarkdownContent, keyword.trim()));
        }
        if (StringUtils.hasText(postType)) {
            String normalizedType = postType.trim().toUpperCase(Locale.ROOT);
            if (!TYPES.contains(normalizedType)) {
                throw invalid("不支持的分享卡片类型");
            }
            query.eq(CommunityPostEntity::getPostType, normalizedType);
        }
        if (tagId != null && tagId > 0) {
            List<CommunityPostTagEntity> relations = postTags.selectList(Wrappers.lambdaQuery(CommunityPostTagEntity.class)
                    .eq(CommunityPostTagEntity::getTagId, tagId)
                    .eq(CommunityPostTagEntity::getDeleted, 0));
            if (relations.isEmpty()) {
                return new CommunityPostPageResponse(0, safePage, safeSize, 0, List.of());
            }
            query.in(CommunityPostEntity::getId, relations.stream().map(CommunityPostTagEntity::getPostId).toList());
        }
        long total = posts.selectCount(query);
        List<CommunityPostEntity> rows = total == 0
                ? List.of()
                : posts.selectList(query.orderByDesc(CommunityPostEntity::getPublishedAt)
                        .last("LIMIT " + ((safePage - 1) * safeSize) + "," + safeSize));
        return new CommunityPostPageResponse(total, safePage, safeSize,
                total == 0 ? 0 : (int) Math.ceil((double) total / safeSize),
                rows.stream().map(post -> toResponse(post, viewerId)).toList());
    }

    @Transactional
    public CommunityPostResponse create(CommunityPostCreateRequest request, long userId) {
        String type = request.postType().trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) {
            throw invalid("不支持的分享卡片类型");
        }
        CommunityPostEntity post = new CommunityPostEntity();
        post.setAuthorId(userId);
        post.setPostType(type);
        post.setTitle(request.title().trim());
        post.setMarkdownContent(blankToNull(request.markdown()));
        post.setPublishedAt(LocalDateTime.now());
        post.setDeleted(0);
        post.setCommentCount(0);
        if ("QUESTION".equals(type) && post.getMarkdownContent() == null) {
            throw invalid("技术问答需要填写 Markdown 内容");
        }
        if ("LINK".equals(type)) {
            if (!StringUtils.hasText(request.linkUrl())) {
                throw invalid("请填写链接");
            }
            post.setLinkUrl(request.linkUrl().trim());
            post.setLinkDomain(links.validate(post.getLinkUrl()));
            post.setLinkComplianceStatus("PASSED");
        }
        posts.insert(post);
        saveTags(post.getId(), request.tagIds());
        if ("VOTE".equals(type)) {
            saveVote(post.getId(), request);
        }
        return toResponse(post, userId);
    }

    @Transactional
    public CommunityPostResponse upload(long postId, MultipartFile file, long userId) {
        CommunityPostEntity post = requirePost(postId);
        requireOwner(post, userId);
        if (file == null || file.isEmpty()) {
            throw invalid("请选择文件");
        }
        String extension = extension(file.getOriginalFilename());
        boolean image = "IMAGE".equals(post.getPostType());
        if (image && !IMAGES.contains(extension)) {
            throw invalid("图片仅支持 JPG、PNG、GIF 或 WebP");
        }
        if (!image && (!"FILE".equals(post.getPostType()) || !FILES.contains(extension))) {
            throw invalid("文件仅支持 txt、pdf、word、ppt、markdown 或 excel；压缩包请改用网盘链接分享");
        }
        long max = image ? imageMax : attachmentMax;
        if (file.getSize() > max) {
            throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "ATTACHMENT_TOO_LARGE",
                    "文件不能超过 " + (max / 1024 / 1024) + "MB");
        }
        long existing = attachments.selectCount(Wrappers.lambdaQuery(CommunityAttachmentEntity.class)
                .eq(CommunityAttachmentEntity::getPostId, postId)
                .eq(CommunityAttachmentEntity::getDeleted, 0));
        if (image && existing >= 9) {
            throw invalid("图片最多支持 9 张");
        }
        if (!image && existing >= 1) {
            throw invalid("文件分享仅支持一个附件");
        }
        try {
            var stored = storage.putStream(new StorageObjectStreamRequest(
                    "community", Long.toString(postId), extension, contentType(extension, file.getContentType()),
                    file.getSize(), file.getInputStream()));
            CommunityAttachmentEntity attachment = new CommunityAttachmentEntity();
            attachment.setPostId(postId);
            attachment.setAttachmentType(image ? "IMAGE" : "FILE");
            attachment.setObjectKey(stored.objectKey());
            attachment.setOriginalName(safeName(file.getOriginalFilename()));
            attachment.setContentType(stored.contentType());
            attachment.setExtension(extension);
            attachment.setSizeBytes(stored.contentLength());
            attachment.setSortOrder((int) existing);
            attachment.setDeleted(0);
            attachments.insert(attachment);
            return toResponse(post, userId);
        } catch (IOException exception) {
            throw invalid("附件读取失败");
        }
    }

    @Transactional
    public CommunityCommentResponse comment(long postId, CommunityCommentCreateRequest request, long userId) {
        requirePost(postId);
        if (request.parentId() != null) {
            CommunityCommentEntity parent = comments.selectById(request.parentId());
            if (parent == null || parent.getDeleted() != 0 || !Objects.equals(parent.getPostId(), postId)) {
                throw invalid("回复目标不存在");
            }
        }
        CommunityCommentEntity comment = new CommunityCommentEntity();
        comment.setPostId(postId);
        comment.setParentId(request.parentId());
        comment.setAuthorId(userId);
        comment.setMarkdownContent(request.markdown().trim());
        comment.setDeleted(0);
        comments.insert(comment);
        posts.update(null, Wrappers.lambdaUpdate(CommunityPostEntity.class)
                .eq(CommunityPostEntity::getId, postId)
                .setSql("comment_count = comment_count + 1"));
        return commentResponse(comment, List.of());
    }

    @Transactional(readOnly = true)
    public List<CommunityCommentResponse> getComments(long postId) {
        requirePost(postId);
        List<CommunityCommentEntity> all = comments.selectList(Wrappers.lambdaQuery(CommunityCommentEntity.class)
                .eq(CommunityCommentEntity::getPostId, postId)
                .eq(CommunityCommentEntity::getDeleted, 0)
                .orderByAsc(CommunityCommentEntity::getCreatetime));
        Map<Long, List<CommunityCommentEntity>> byParent = all.stream()
                .collect(Collectors.groupingBy(comment -> comment.getParentId() == null ? 0L : comment.getParentId()));
        return byParent.getOrDefault(0L, List.of()).stream()
                .map(comment -> commentResponse(comment, buildComments(comment.getId(), byParent)))
                .toList();
    }

    @Transactional
    public CommunityPostResponse vote(long postId, CommunityVoteRequest request, long userId) {
        CommunityPostEntity post = requirePost(postId);
        if (!"VOTE".equals(post.getPostType())) {
            throw invalid("该分享不是投票");
        }
        CommunityVoteEntity vote = votes.selectById(postId);
        if (options.countVote(postId, userId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_VOTED", "你已完成本次投票");
        }
        List<CommunityVoteOptionEntity> choices = options.selectBatchIds(request.optionIds());
        if (choices.size() != new LinkedHashSet<>(request.optionIds()).size()
                || choices.stream().anyMatch(option -> !Objects.equals(option.getPostId(), postId))) {
            throw invalid("投票选项无效");
        }
        if (!vote.getAllowMultiple() && choices.size() != 1) {
            throw invalid("该投票仅支持单选");
        }
        CommunityVoteRecord record = new CommunityVoteRecord();
        record.setPostId(postId);
        record.setUserId(userId);
        options.insertVote(record);
        for (CommunityVoteOptionEntity option : choices) {
            options.insertSelection(record.getId(), option.getId());
            options.increment(option.getId());
        }
        options.incrementTotal(postId);
        return toResponse(post, userId);
    }

    @Transactional
    public void delete(long postId, long userId) {
        CommunityPostEntity post = requirePost(postId);
        UserEntity user = users.selectById(userId);
        if (user == null || user.getUserRole() != UserRole.SUPER_ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "POST_DELETE_FORBIDDEN", "仅管理员可删除分享");
        }
        attachments.selectList(Wrappers.lambdaQuery(CommunityAttachmentEntity.class)
                        .eq(CommunityAttachmentEntity::getPostId, postId)
                        .eq(CommunityAttachmentEntity::getDeleted, 0))
                .forEach(attachment -> {
                    try {
                        storage.delete(attachment.getObjectKey());
                    } catch (Exception ignored) {
                        // 数据逻辑删除必须继续执行，残留对象由存储清理任务兜底。
                    }
                });
        posts.deleteById(post.getId());
    }

    @Transactional(readOnly = true)
    public CommunityAttachmentContent content(long attachmentId) {
        CommunityAttachmentEntity attachment = requireAttachment(attachmentId);
        requirePost(attachment.getPostId());
        StoredObjectStream object = storage.open(attachment.getObjectKey());
        return new CommunityAttachmentContent(object.inputStream(), fallbackContentType(object.contentType(), attachment.getContentType()),
                object.contentLength(), attachment.getOriginalName());
    }

    @Transactional(readOnly = true)
    public CommunityAttachmentPreviewResponse preview(long attachmentId) {
        CommunityAttachmentEntity attachment = requireAttachment(attachmentId);
        requirePost(attachment.getPostId());
        if (!TEXT_PREVIEWS.contains(attachment.getExtension())) {
            throw invalid("该文件不支持文本预览");
        }
        if (attachment.getSizeBytes() > previewMax) {
            throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "PREVIEW_TOO_LARGE",
                    "该文件超过 " + (previewMax / 1024 / 1024) + "MB，无法在线预览");
        }
        StoredObjectContent object = storage.get(attachment.getObjectKey());
        String text = extractPreview(object.content(), attachment.getExtension());
        boolean truncated = text.length() > PREVIEW_CHARACTER_LIMIT;
        return new CommunityAttachmentPreviewResponse(attachment.getOriginalName(),
                truncated ? text.substring(0, PREVIEW_CHARACTER_LIMIT) : text, truncated);
    }

    private CommunityAttachmentEntity requireAttachment(long id) {
        CommunityAttachmentEntity attachment = attachments.selectById(id);
        if (attachment == null || attachment.getDeleted() != 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ATTACHMENT_NOT_FOUND", "附件不存在");
        }
        return attachment;
    }

    private CommunityPostEntity requirePost(long id) {
        CommunityPostEntity post = posts.selectById(id);
        if (post == null || post.getDeleted() != 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "分享不存在或已删除");
        }
        return post;
    }

    private void requireOwner(CommunityPostEntity post, long userId) {
        if (!Objects.equals(post.getAuthorId(), userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ATTACHMENT_FORBIDDEN", "仅发布者可以上传附件");
        }
    }

    private void saveTags(long postId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Set<Long> unique = new LinkedHashSet<>(ids);
        if (unique.size() != ids.size() || unique.size() > 5) {
            throw invalid("最多选择 5 个不重复的知识标签");
        }
        List<KnowledgeTagEntity> found = tags.selectBatchIds(unique);
        if (found.size() != unique.size() || found.stream().anyMatch(tag -> tag.getDeleted() != 0)) {
            throw invalid("包含不存在的知识标签");
        }
        for (Long tagId : unique) {
            CommunityPostTagEntity relation = new CommunityPostTagEntity();
            relation.setPostId(postId);
            relation.setTagId(tagId);
            relation.setDeleted(0);
            postTags.insert(relation);
        }
    }

    private void saveVote(long postId, CommunityPostCreateRequest request) {
        if (!StringUtils.hasText(request.voteQuestion()) || request.voteOptions() == null || request.voteOptions().size() < 2) {
            throw invalid("投票至少需要问题和两个选项");
        }
        CommunityVoteEntity vote = new CommunityVoteEntity();
        vote.setPostId(postId);
        vote.setQuestion(request.voteQuestion().trim());
        vote.setAllowMultiple(Boolean.TRUE.equals(request.allowMultiple()));
        vote.setAnonymous(Boolean.TRUE.equals(request.anonymous()));
        vote.setVoteCount(0);
        votes.insert(vote);
        int order = 0;
        for (String optionText : request.voteOptions()) {
            if (!StringUtils.hasText(optionText)) {
                throw invalid("投票选项不能为空");
            }
            CommunityVoteOptionEntity option = new CommunityVoteOptionEntity();
            option.setPostId(postId);
            option.setOptionText(optionText.trim());
            option.setSortOrder(order++);
            option.setVoteCount(0);
            options.insert(option);
        }
    }

    private CommunityPostResponse toResponse(CommunityPostEntity post, long viewerId) {
        UserEntity author = users.selectById(post.getAuthorId());
        if (author == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "POST_AUTHOR_NOT_FOUND", "分享作者不存在");
        }
        List<CommunityPostResponse.Tag> postTagsResponse = postTags.selectList(Wrappers.lambdaQuery(CommunityPostTagEntity.class)
                        .eq(CommunityPostTagEntity::getPostId, post.getId())
                        .eq(CommunityPostTagEntity::getDeleted, 0))
                .stream()
                .map(relation -> tags.selectById(relation.getTagId()))
                .filter(Objects::nonNull)
                .map(tag -> new CommunityPostResponse.Tag(tag.getId(), tag.getName(), tag.getLevel()))
                .toList();
        List<CommunityPostResponse.Attachment> attachmentResponses = attachments.selectList(Wrappers.lambdaQuery(CommunityAttachmentEntity.class)
                        .eq(CommunityAttachmentEntity::getPostId, post.getId())
                        .eq(CommunityAttachmentEntity::getDeleted, 0)
                        .orderByAsc(CommunityAttachmentEntity::getSortOrder))
                .stream()
                .map(attachment -> new CommunityPostResponse.Attachment(attachment.getId(), attachment.getOriginalName(),
                        attachment.getContentType(), attachment.getExtension(), attachment.getSizeBytes(),
                        attachment.getAttachmentType(), previewable(attachment.getExtension())))
                .toList();
        CommunityPostResponse.Vote voteResponse = null;
        if ("VOTE".equals(post.getPostType())) {
            CommunityVoteEntity vote = votes.selectById(post.getId());
            List<CommunityPostResponse.VoteOption> voteOptions = options.selectList(Wrappers.lambdaQuery(CommunityVoteOptionEntity.class)
                            .eq(CommunityVoteOptionEntity::getPostId, post.getId())
                            .orderByAsc(CommunityVoteOptionEntity::getSortOrder))
                    .stream().map(option -> new CommunityPostResponse.VoteOption(option.getId(), option.getOptionText(), option.getVoteCount())).toList();
            voteResponse = new CommunityPostResponse.Vote(vote.getQuestion(), vote.getAllowMultiple(), vote.getAnonymous(),
                    vote.getVoteCount(), options.countVote(post.getId(), viewerId) > 0, voteOptions);
        }
        UserEntity viewer = users.selectById(viewerId);
        boolean canDelete = viewer != null && viewer.getUserRole() == UserRole.SUPER_ADMIN;
        return new CommunityPostResponse(post.getId(), post.getPostType(), post.getTitle(), post.getMarkdownContent(),
                post.getLinkUrl(), post.getLinkDomain(), new CommunityPostResponse.Author(author.getId(), author.getUsername(), author.getAvatarUrl()),
                postTagsResponse, attachmentResponses, voteResponse, post.getCommentCount(), post.getPublishedAt(), canDelete);
    }

    private List<CommunityCommentResponse> buildComments(Long parentId, Map<Long, List<CommunityCommentEntity>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(comment -> commentResponse(comment, buildComments(comment.getId(), byParent)))
                .toList();
    }

    private CommunityCommentResponse commentResponse(CommunityCommentEntity comment, List<CommunityCommentResponse> children) {
        UserEntity author = users.selectById(comment.getAuthorId());
        if (author == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "COMMENT_AUTHOR_NOT_FOUND", "评论作者不存在");
        }
        return new CommunityCommentResponse(comment.getId(), comment.getParentId(), comment.getMarkdownContent(),
                new CommunityCommentResponse.Author(author.getId(), author.getUsername(), author.getAvatarUrl()), comment.getCreatetime(), children);
    }

    private String extractPreview(byte[] content, String extension) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(content)) {
            return switch (extension) {
                case "txt", "md", "markdown" -> new String(content, StandardCharsets.UTF_8);
                case "docx" -> extractDocx(input);
                case "doc" -> extractDoc(input);
                case "pptx" -> extractPptx(input);
                case "ppt" -> extractPpt(input);
                case "xlsx", "xls" -> extractWorkbook(input);
                default -> throw invalid("该文件不支持文本预览");
            };
        } catch (IOException | RuntimeException exception) {
            throw invalid("无法解析该文档预览");
        }
    }

    private String extractDocx(ByteArrayInputStream input) throws IOException {
        try (XWPFDocument document = new XWPFDocument(input); XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDoc(ByteArrayInputStream input) throws IOException {
        try (HWPFDocument document = new HWPFDocument(input); WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractPptx(ByteArrayInputStream input) throws IOException {
        try (XMLSlideShow show = new XMLSlideShow(input)) {
            StringBuilder text = new StringBuilder();
            int index = 1;
            for (XSLFSlide slide : show.getSlides()) {
                text.append("\n# 幻灯片 ").append(index++).append('\n');
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape && StringUtils.hasText(textShape.getText())) {
                        text.append(textShape.getText()).append('\n');
                    }
                }
            }
            return text.toString();
        }
    }

    private String extractPpt(ByteArrayInputStream input) throws IOException {
        try (HSLFSlideShow show = new HSLFSlideShow(input)) {
            StringBuilder text = new StringBuilder();
            int index = 1;
            for (HSLFSlide slide : show.getSlides()) {
                text.append("\n# 幻灯片 ").append(index++).append('\n');
                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape textShape && StringUtils.hasText(textShape.getText())) {
                        text.append(textShape.getText()).append('\n');
                    }
                }
            }
            return text.toString();
        }
    }

    private String extractWorkbook(ByteArrayInputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            StringBuilder text = new StringBuilder();
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            for (Sheet sheet : workbook) {
                text.append("\n# ").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    row.forEach(cell -> cells.add(formatter.formatCellValue(cell)));
                    if (!cells.isEmpty()) {
                        text.append(String.join(" | ", cells)).append('\n');
                    }
                }
            }
            return text.toString();
        }
    }

    private static boolean previewable(String extension) {
        return IMAGES.contains(extension) || Set.of("pdf").contains(extension) || TEXT_PREVIEWS.contains(extension);
    }

    private static String extension(String name) {
        if (name == null || !name.contains(".")) {
            throw invalid("文件缺少扩展名");
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static String safeName(String name) {
        return name == null ? "attachment" : name.replaceAll("[\\r\\n\\\\/]", "_");
    }

    private static String contentType(String extension, String declared) {
        if (IMAGES.contains(extension) && StringUtils.hasText(declared)) {
            return declared;
        }
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain;charset=UTF-8";
            case "md", "markdown" -> "text/markdown;charset=UTF-8";
            default -> "application/octet-stream";
        };
    }

    private static String fallbackContentType(String stored, String expected) {
        return StringUtils.hasText(stored) ? stored : expected;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_COMMUNITY_POST", message);
    }
}
