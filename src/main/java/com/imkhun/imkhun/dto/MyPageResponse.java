package com.imkhun.imkhun.dto;

public record MyPageResponse(Long id, String email, String username, String nickname,
                             String phone, String profileImage, String createdAt) {
}