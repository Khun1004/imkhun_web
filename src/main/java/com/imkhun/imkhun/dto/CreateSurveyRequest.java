package com.imkhun.imkhun.dto;

// 별점은 각각 1~5
public record CreateSurveyRequest(int contentRating, int teacherRating, int materialRating,
                                  int overallRating, String comment, boolean isAnonymous) {
}