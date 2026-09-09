package com.imkhun.imkhun.dto;

import java.util.List;

public record AttendanceSummaryResponse(long presentCount, long absentCount, long makeupCount,
                                        List<AttendanceRecordResponse> records) {
}