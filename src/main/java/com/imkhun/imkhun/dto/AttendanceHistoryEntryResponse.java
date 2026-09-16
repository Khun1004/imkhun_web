package com.imkhun.imkhun.dto;

public record AttendanceHistoryEntryResponse(Long id, Long applicationId, String studentUsername, String courseName,
                                             String classDate, String status, boolean checkedInByStudent) {
}