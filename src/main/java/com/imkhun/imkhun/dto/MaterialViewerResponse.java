package com.imkhun.imkhun.dto;

// 관리자 화면에서 "이 영상을 누가 봤는지" 보여줄 때 한 줄
public record MaterialViewerResponse(String username, String nickname, String viewedAt) {
}