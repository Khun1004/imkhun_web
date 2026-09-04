package com.imkhun.imkhun.dto;

public record PostResponse(Long id, String nickname, String topic, String category, String title,
                           String content, String createdAt, int likeCount, int dislikeCount,
                           int commentCount, String myReaction) {
}