package com.imkhun.imkhun.dto;

public record CreateLanguageMaterialRequest(String language, int sortOrder, String title, String description,
                                            String badge, String imageUrl, String fileUrl, String fileName) {
}