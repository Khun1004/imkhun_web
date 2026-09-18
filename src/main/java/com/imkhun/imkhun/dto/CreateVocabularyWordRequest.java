package com.imkhun.imkhun.dto;

public record CreateVocabularyWordRequest(String language, String word, String meaning, String example) {
}