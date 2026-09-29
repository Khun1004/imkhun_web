package com.imkhun.imkhun.dto;

// type: "ATTENDANCE_STREAK" | "ATTENDANCE_COUNT" | "VOCAB_WORDS" | "ASSIGNMENT_COUNT"
public record CreateSharedGoalRequest(String friendUsername, String type, String title, int targetValue) {
}