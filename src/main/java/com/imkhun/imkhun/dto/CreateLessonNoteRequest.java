package com.imkhun.imkhun.dto;

// classDate는 "2026-09-15" 형식 문자열이거나, 비워두면 그냥 메모로 취급함
public record CreateLessonNoteRequest(String classDate, String content) {
}