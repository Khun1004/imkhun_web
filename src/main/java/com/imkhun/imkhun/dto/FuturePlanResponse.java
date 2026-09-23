package com.imkhun.imkhun.dto;

import java.util.List;

public record FuturePlanResponse(String introText, String ctaTitle, String ctaText, List<FuturePlanItemResponse> items) {
}