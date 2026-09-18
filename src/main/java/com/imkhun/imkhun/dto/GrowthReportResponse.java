package com.imkhun.imkhun.dto;

import java.util.List;

public record GrowthReportResponse(List<LevelRecordResponse> levelHistory,
                                   double attendanceRate, long totalAttendanceRecords,
                                   double assignmentCompletionRate, long totalAssignments, long completedAssignments) {
}