package com.imkhun.imkhun.dto;

// 관리자 "학생 메시지" 화면의 왼쪽 대화방 목록 — 학생 한 명당 한 줄
public record AdminMessageThreadResponse(String username, String nickname, String lastMessage,
                                         String lastMessageAt, long unreadCount) {
}