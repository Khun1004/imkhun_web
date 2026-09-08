package com.imkhun.imkhun.dto;

public record CreateTimetableEntryRequest(String day, String startTime, String endTime,
                                          String courseName, String colorType) {
}