package com.imkhun.imkhun.dto;

// type: "CLASS" | "ASSIGNMENT"
// CLASS일 때 status: PRESENT/LATE/ABSENT/MAKEUP/null(아직 기록 없음, 예정된 수업)
// ASSIGNMENT일 때 status: "완료" | "미완료"
public record CalendarEventResponse(String date, String type, String title, String status) {
}