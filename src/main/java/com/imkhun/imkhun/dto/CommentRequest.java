package com.imkhun.imkhun.dto;

// parentCommentId가 null이면 글에 바로 다는 댓글, 값이 있으면 그 댓글에 대한 답글이에요
public record CommentRequest(String content, Long parentCommentId) {
}