package com.aitechskill.book.community.domain.response;
import java.time.LocalDateTime;
import java.util.List;
public record CommunityPostResponse(long id,String postType,String title,String markdown,String linkUrl,String linkDomain,
 Author author,List<Tag> tags,List<Attachment> attachments,Vote vote,int commentCount,LocalDateTime publishedAt,boolean canDelete) {
 public record Author(long id,String username,String avatarUrl) { }
 public record Tag(long id,String name,int level) { }
 public record Attachment(long id,String originalName,String contentType,String extension,long sizeBytes,String attachmentType,boolean previewable) { }
 public record Vote(String question,boolean allowMultiple,boolean anonymous,int voteCount,boolean voted,List<VoteOption> options) { }
 public record VoteOption(long id,String text,int voteCount) { }
}
