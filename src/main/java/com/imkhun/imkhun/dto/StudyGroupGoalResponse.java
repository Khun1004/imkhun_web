package com.imkhun.imkhun.dto;

import java.util.List;

public record StudyGroupGoalResponse(Long id, String type, String title, int targetValue,
                                     List<StudyGroupGoalMemberProgressResponse> memberProgress,
                                     boolean allAchieved, String createdAt) {
}