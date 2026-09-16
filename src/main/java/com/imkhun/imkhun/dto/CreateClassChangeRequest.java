package com.imkhun.imkhun.dto;

// requestType: "CANCEL" / "RESCHEDULE". requestedDate는 RESCHEDULE일 때만 채움 ("2026-09-20" 형식)
public record CreateClassChangeRequest(String requestType, String classDate, String requestedDate, String reason) {
}