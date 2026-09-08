package com.imkhun.imkhun.dto;

public record TimetableEntryResponse(Long id, String day, String startTime, String endTime,
                                     String courseName, String colorType) {
}