package com.imkhun.imkhun.dto;

// 생일 등록/수정 — 둘 다 null로 보내면 등록 해제(생일 알림 안 받음)
public record UpdateBirthdayRequest(Integer birthMonth, Integer birthDay) {
}