package com.imkhun.imkhun.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}