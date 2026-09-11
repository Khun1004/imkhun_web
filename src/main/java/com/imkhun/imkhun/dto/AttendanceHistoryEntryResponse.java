package com.imkhun.imkhun.dto;

public record AttendanceHistoryEntryResponse(Long id, String studentUsername, String courseName,
                                             String classDate, String status, boolean checkedInByStudent) {
}