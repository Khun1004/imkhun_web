package com.imkhun.imkhun.dto;

public record AdminLoginRequest(String email, String phone, String username, String password) {
}