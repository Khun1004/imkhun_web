package com.imkhun.imkhun.dto;

// recordedDate는 "2026-09-16" 형식 문자열이거나 비워두면 오늘로 처리됨
public record CreateLevelRecordRequest(String level, String note, String recordedDate) {
}