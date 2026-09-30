package com.imkhun.imkhun.dto;

// type: "ATTENDANCE_STREAK" | "ATTENDANCE_COUNT" | "VOCAB_WORDS" | "ASSIGNMENT_COUNT"
public record CreateStudyGroupGoalRequest(String type, String title, int targetValue) {
}