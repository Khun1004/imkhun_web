package com.imkhun.imkhun.dto;

// 랭킹보드 한 줄. 전체 랭킹은 nickname이 항상 null(이름을 안 보여줌), 친구 한정 랭킹은
// 서로 아는 사이라 nickname을 채워서 내려줌. isMe로 본인 줄만 화면에서 강조 표시함
public record LeaderboardEntryResponse(int rank, int value, boolean isMe, String nickname) {
}