package com.aitechskill.book.community.domain.response;
import java.util.List;
public record CommunityPostPageResponse(long total,int page,int size,int totalPages,List<CommunityPostResponse> items) { }
