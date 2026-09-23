package com.imkhun.imkhun.dto;

import java.util.List;

// 부모님이 로그인 없이 링크로 보는 요약 리포트 — 출석/성적 요약만 담고, 개인 메시지·결제 정보 등은 절대 안 담음
public record ParentReportResponse(String studentNickname, int attendanceStreak, double attendanceRate,
                                   long totalAttendanceRecords, double assignmentCompletionRate,
                                   long totalAssignments, long completedAssignments,
                                   List<LevelRecordResponse> levelHistory) {
}