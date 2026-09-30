package com.imkhun.imkhun.dto;

// studyType: "TOGETHER"(1:1 — day/startTime/endTime을 요일·시간으로 씀) / "VIDEO"(온라인 — startTime/endTime을 시작월·종료월로 씀, day는 null)
public record CreateTimetableEntryRequest(String studyType, String day, String startTime, String endTime,
                                          String courseName, String colorType) {
}