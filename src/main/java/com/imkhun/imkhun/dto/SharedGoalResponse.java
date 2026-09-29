package com.imkhun.imkhun.dto;

// status: "PENDING" | "ACTIVE" | "DECLINED". 보는 사람(나) 기준으로 나/친구를 나눠서 내려줌
public record SharedGoalResponse(Long id, String type, String title, int targetValue, String status,
                                 String friendUsername, String friendNickname,
                                 int myProgress, boolean myAchieved,
                                 int friendProgress, boolean friendAchieved,
                                 boolean isCreator, String createdAt) {
}