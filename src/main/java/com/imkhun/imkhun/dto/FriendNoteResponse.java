package com.imkhun.imkhun.dto;

public record FriendNoteResponse(Long id, String senderUsername, String senderNickname, String message, String createdAt) {
}