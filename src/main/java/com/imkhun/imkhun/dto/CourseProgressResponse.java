package com.imkhun.imkhun.dto;

// 마이페이지 "나의 성장"에서 보여주는 언어별 진도율. percent는 그 언어의 단어장 전체 단어 수 대비
// "외웠어요"로 표시해둔 단어 수의 비율이고, 단어가 하나도 없으면 null (진행바를 안 그림).
public record CourseProgressResponse(String language, String courseNames,
                                     String currentLevel, long learnedWords, long totalWords, Integer percent) {
}