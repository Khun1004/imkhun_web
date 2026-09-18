package com.imkhun.imkhun.dto;

import java.util.List;

public record AttendanceStreakResponse(int currentStreak, double thisMonthAttendanceRate, List<String> badges) {
}