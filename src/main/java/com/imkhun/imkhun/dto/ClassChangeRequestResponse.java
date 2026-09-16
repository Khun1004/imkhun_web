package com.imkhun.imkhun.dto;

public record ClassChangeRequestResponse(Long id, Long applicationId, String courseName, String studentUsername,
                                         String requestType, String classDate, String requestedDate,
                                         String reason, String status, String adminReply, String createdAt) {
}