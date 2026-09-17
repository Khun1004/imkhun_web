package com.imkhun.imkhun.dto;

// "2026-12-31" 형식. 비워두면(null) 리마인더 대상에서 빠짐
public record UpdateEnrollmentEndDateRequest(String enrollmentEndDate) {
}