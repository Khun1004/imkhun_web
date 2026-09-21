package com.imkhun.imkhun.dto;

public record AssignmentResponse(Long id, Long applicationId, String courseName, String title, String description,
                                 String dueDate, boolean completed, String createdAt, boolean hasSubmission) {
}