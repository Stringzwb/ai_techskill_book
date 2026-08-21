package com.aitechskill.book.community.domain.response;
import java.time.LocalDateTime;
import java.util.List;
public record CommunityCommentResponse(long id,Long parentId,String markdown,Author author,LocalDateTime createdAt,List<CommunityCommentResponse> children) {
 public record Author(long id,String username,String avatarUrl) { }
}
