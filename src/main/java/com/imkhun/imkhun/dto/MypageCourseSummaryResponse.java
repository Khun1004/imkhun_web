package com.imkhun.imkhun.dto;

// 마이페이지 요약 카드에서 강의 하나당 보여주는 한 줄 정보.
// daysUntilDue: 다음 납부(수강 종료)일까지 남은 일수. 0이면 오늘, 음수면 이미 지남. 종료일을 안 정해뒀으면 null.
public record MypageCourseSummaryResponse(Long applicationId, String courseName, String studentNumber,
                                          long presentCount, long lateCount, long absentCount, long makeupCount,
                                          String enrollmentEndDate, Long daysUntilDue, boolean receiptAvailable) {
}