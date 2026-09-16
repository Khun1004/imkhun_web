package com.imkhun.imkhun.dto;

// approved가 true면 승인, false면 거절. adminReply는 선택
public record RespondClassChangeRequest(boolean approved, String adminReply) {
}