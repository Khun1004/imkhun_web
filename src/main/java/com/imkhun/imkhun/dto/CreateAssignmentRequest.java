package com.imkhun.imkhun.dto;

// dueDate는 "2026-09-20" 형식 문자열, 없으면 null
public record CreateAssignmentRequest(String title, String description, String dueDate) {
}