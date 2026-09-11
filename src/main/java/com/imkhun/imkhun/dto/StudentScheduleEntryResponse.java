package com.imkhun.imkhun.dto;

// classDays는 "MON,FRI"처럼 쉼표로 구분된 요일 코드
public record StudentScheduleEntryResponse(String courseName, String classDays, String classTime) {
}