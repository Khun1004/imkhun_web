package com.imkhun.imkhun.dto;

import java.util.List;

public record MypageSummaryResponse(List<MypageCourseSummaryResponse> courses,
                                    long totalPresentCount, long totalLateCount,
                                    long totalAbsentCount, long totalMakeupCount) {
}