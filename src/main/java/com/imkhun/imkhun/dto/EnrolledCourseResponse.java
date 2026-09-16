package com.imkhun.imkhun.dto;

// "수업 변경 요청" 같은 곳에서 씀 — KWZM 자료 초대 여부랑 상관없이 승인된 신청이면 다 보여줌
public record EnrolledCourseResponse(Long applicationId, String courseName) {
}