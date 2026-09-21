package com.imkhun.imkhun.dto;

public record VocabularySetResponse(Long id, String language, String name, String category,
                                    int quizTimeLimitMinutes, long wordCount) {
}