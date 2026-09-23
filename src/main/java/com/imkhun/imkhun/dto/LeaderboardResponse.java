package com.imkhun.imkhun.dto;

import java.util.List;

// top: 상위 랭킹 목록 (이름 없이 등수/점수만). myRank/myValue: 내가 top 안에 없어도 내 등수를 알 수 있게
public record LeaderboardResponse(List<LeaderboardEntryResponse> top, int myRank, int myValue, int totalStudents) {
}