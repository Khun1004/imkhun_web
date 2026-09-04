package com.imkhun.imkhun.dto;

public record CommentResponse(Long id, String nickname, String content, String createdAt, boolean isAdmin,
                              Long parentCommentId, int likeCount, int dislikeCount, String myReaction) {
}