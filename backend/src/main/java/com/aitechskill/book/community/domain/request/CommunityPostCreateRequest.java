package com.aitechskill.book.community.domain.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
public record CommunityPostCreateRequest(
 @NotBlank @Size(max=16) String postType, @NotBlank @Size(max=160) String title,
 @Size(max=50000) String markdown, @Size(max=2048) String linkUrl,
 @Size(max=5) List<Long> tagIds, @Size(max=300) String voteQuestion,
 @Size(max=12) List<@Size(max=160) String> voteOptions, Boolean allowMultiple, Boolean anonymous) { }
