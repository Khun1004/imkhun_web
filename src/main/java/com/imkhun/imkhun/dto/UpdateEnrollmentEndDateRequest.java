package com.imkhun.imkhun.dto;

// 둘 다 "2026-12-31" 형식의 문자열. 각각 비워두면(null) 그 값은 안 정해진 것으로 처리됨.
// enrollmentEndDate를 비워두면 재등록 리마인더 대상에서도 빠짐
public record UpdateEnrollmentEndDateRequest(String enrollmentStartDate, String enrollmentEndDate) {
}