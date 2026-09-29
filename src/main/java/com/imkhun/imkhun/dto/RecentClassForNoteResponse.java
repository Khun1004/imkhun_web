package com.imkhun.imkhun.dto;

// 마이페이지 > 미니 노트 탭에서 "노트 남기기" 대상으로 고를 수 있는 최근 출석 수업 목록
public record RecentClassForNoteResponse(Long attendanceRecordId, String courseName, String classDate,
                                         boolean hasNote, Long existingNoteId, String existingNoteContent) {
}