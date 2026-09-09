package com.imkhun.imkhun.dto;

// classDays는 "TUE,WED"처럼 쉼표로 구분된 요일 코드 (MON/TUE/WED/THU/FRI/SAT/SUN)
public record UpdateScheduleRequest(String classDays, String classTime) {
}