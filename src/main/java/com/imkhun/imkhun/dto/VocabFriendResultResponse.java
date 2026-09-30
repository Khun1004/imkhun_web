package com.imkhun.imkhun.dto;

// 단어 퀴즈 대결 한 줄 — 같은 Part(단어 세트)를 나 또는 친구가 풀었을 때의 최근 점수.
// 아직 그 Part를 안 풀어본 친구는 score/totalQuestions/completedAt이 전부 null로 내려감
public record VocabFriendResultResponse(String username, String nickname, boolean isMe,
                                        Integer score, Integer totalQuestions, String completedAt) {
}