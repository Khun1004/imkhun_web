package com.imkhun.imkhun.dto;

public record SentFileResponse(Long id, String category, String fileName, String fileData,
                               String courseName, String createdAt) {
}