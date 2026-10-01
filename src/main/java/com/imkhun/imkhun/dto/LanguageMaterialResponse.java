package com.imkhun.imkhun.dto;

public record LanguageMaterialResponse(Long id, String language, int sortOrder, String title, String description,
                                       String badge, String imageUrl, String fileUrl, String fileName) {
}