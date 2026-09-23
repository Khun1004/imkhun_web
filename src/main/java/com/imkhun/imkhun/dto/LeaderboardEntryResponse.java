package com.imkhun.imkhun.dto;

// 랭킹보드 한 줄 — 이름은 안 보여주고 등수/점수만 보여줌. isMe로 본인 줄만 화면에서 강조 표시함
public record LeaderboardEntryResponse(int rank, int value, boolean isMe) {
}