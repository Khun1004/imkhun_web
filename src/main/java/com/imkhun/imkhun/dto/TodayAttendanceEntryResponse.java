package com.imkhun.imkhun.dto;

// status는 "PRESENT"/"LATE"/"ABSENT"/"MAKEUP" 중 하나, 아직 아무 기록도 없으면 null
public record TodayAttendanceEntryResponse(Long applicationId, String studentNickname, String courseName,
                                           String classTime, String status, Long recordId,
                                           boolean checkedInByStudent) {
}