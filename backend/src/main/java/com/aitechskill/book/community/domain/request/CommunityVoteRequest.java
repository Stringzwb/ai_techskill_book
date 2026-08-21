package com.aitechskill.book.community.domain.request;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
public record CommunityVoteRequest(@NotEmpty List<Long> optionIds) { }
