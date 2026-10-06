package com.imkhun.imkhun.dto;

// 마이페이지에서 내가 등록해둔 생일(월/일)을 보여줄 때 씀 — 연도는 저장 안 함
public record BirthdayResponse(Integer birthMonth, Integer birthDay) {
}