package com.imkhun.imkhun.dto;

import java.util.List;

// 학생이 로그인하면 바로 보는 "오늘 할 일" 요약
public record TodaySummaryResponse(CheckinStatusResponse checkinStatus, List<AssignmentResponse> upcomingAssignments,
                                   long unreadNotificationCount) {
}