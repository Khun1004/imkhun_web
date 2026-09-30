package com.imkhun.imkhun.dto;

// status: "INVITED" | "ACTIVE" | "DECLINED" | "LEFT"
public record StudyGroupMemberResponse(String username, String nickname, String status, boolean isCreator) {
}