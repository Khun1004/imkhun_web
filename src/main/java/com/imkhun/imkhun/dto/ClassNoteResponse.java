package com.imkhun.imkhun.dto;

public record ClassNoteResponse(Long id, Long attendanceRecordId, String courseName, String classDate,
                                String content, String createdAt, String updatedAt) {
}