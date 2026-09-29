package com.imkhun.imkhun.dto;

// classDays는 "TUE,WED"처럼 쉼표로 구분된 요일 코드 (MON/TUE/WED/THU/FRI/SAT/SUN)
// classEndTime은 "11:00"처럼 자유 형식, 없으면 null (표시용으로만 쓰이고 출석 로직은 classTime 기준)
public record UpdateScheduleRequest(String classDays, String classTime, String classEndTime) {
}