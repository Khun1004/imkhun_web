package com.imkhun.imkhun.dto;

// classDays/classTime은 선택 입력이에요 (학생이 원하는 요일·시간을 미리 알려줄 수 있게)
public record CreateApplicationRequest(String studyType, String courseName, String contact, String memo,
                                       String classDays, String classTime) {
}