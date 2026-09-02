package com.imkhun.imkhun.dto;

public record StudentLoginRequest(String username, String password, String email, String studentNumber) {
}