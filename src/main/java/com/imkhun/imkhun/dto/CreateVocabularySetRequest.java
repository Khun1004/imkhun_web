package com.imkhun.imkhun.dto;

public record CreateVocabularySetRequest(String language, String name, String category, int quizTimeLimitMinutes) {
}