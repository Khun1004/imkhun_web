package com.imkhun.imkhun.dto;

public record SurveySummaryResponse(long totalResponses, double avgContentRating, double avgTeacherRating,
                                    double avgMaterialRating, double avgOverallRating) {
}