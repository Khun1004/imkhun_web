package com.imkhun.imkhun.dto;

public record BadgeResponse(String key, String name, String description, String icon, String category,
                            boolean earned, String earnedAt, int currentValue, int targetValue) {
}