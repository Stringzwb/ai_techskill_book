package com.aitechskill.book.community.domain.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CommunityCommentCreateRequest(Long parentId, @NotBlank @Size(max=8000) String markdown) { }
