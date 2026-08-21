package com.aitechskill.book.community.controller;

import com.aitechskill.book.auth.utils.UserContextHolder;
import com.aitechskill.book.community.domain.request.*;
import com.aitechskill.book.community.domain.response.*;
import com.aitechskill.book.community.service.CommunityService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.core.io.InputStreamResource;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 分享库的发布、附件、投票和多层讨论接口。 */
@RestController @RequestMapping("/api/community")
public class CommunityController {
 private final CommunityService service;
 public CommunityController(CommunityService service){this.service=service;}
 @GetMapping("/posts") public CommunityPostPageResponse list(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long tagId,@RequestParam(required=false) String postType,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size){return service.list(keyword,tagId,postType,page,size,UserContextHolder.requireUserId());}
 @PostMapping("/posts") public CommunityPostResponse create(@Valid @RequestBody CommunityPostCreateRequest request){return service.create(request,UserContextHolder.requireUserId());}
 @PostMapping(value="/posts/{id}/attachments",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public CommunityPostResponse upload(@PathVariable long id,@RequestPart("file") MultipartFile file){return service.upload(id,file,UserContextHolder.requireUserId());}
 @GetMapping("/posts/{id}/comments") public List<CommunityCommentResponse> comments(@PathVariable long id){return service.getComments(id);}
 @PostMapping("/posts/{id}/comments") public CommunityCommentResponse comment(@PathVariable long id,@Valid @RequestBody CommunityCommentCreateRequest request){return service.comment(id,request,UserContextHolder.requireUserId());}
 @PostMapping("/posts/{id}/vote") public CommunityPostResponse vote(@PathVariable long id,@Valid @RequestBody CommunityVoteRequest request){return service.vote(id,request,UserContextHolder.requireUserId());}
 @DeleteMapping("/posts/{id}") public ResponseEntity<Void> delete(@PathVariable long id){service.delete(id,UserContextHolder.requireUserId());return ResponseEntity.noContent().build();}
 @GetMapping("/attachments/{id}/content") public ResponseEntity<InputStreamResource> content(@PathVariable long id,@RequestParam(defaultValue="false") boolean download){CommunityAttachmentContent o=service.content(id);return ResponseEntity.ok().contentType(mediaType(o.contentType())).contentLength(o.contentLength()).cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate()).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.builder(download?"attachment":"inline").filename(o.originalName(),StandardCharsets.UTF_8).build().toString()).body(new InputStreamResource(o.inputStream()));}
 @GetMapping("/attachments/{id}/preview") public CommunityAttachmentPreviewResponse preview(@PathVariable long id){return service.preview(id);}
 private MediaType mediaType(String value){try{return MediaType.parseMediaType(value);}catch(IllegalArgumentException ignored){return MediaType.APPLICATION_OCTET_STREAM;}}
}
