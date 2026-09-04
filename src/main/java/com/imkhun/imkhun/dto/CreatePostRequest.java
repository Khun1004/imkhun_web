package com.imkhun.imkhun.dto;

public record CreatePostRequest(String topic, String category, String title, String content) {
}