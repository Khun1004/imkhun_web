package com.imkhun.imkhun.dto;

import java.util.List;

public record AssignmentSubmissionResponse(Long id, Long assignmentId, String studentUsername, String assignmentTitle,
                                           String courseName, String textAnswer, List<AttachmentItem> attachments,
                                           String adminComment, String submittedAt) {
}