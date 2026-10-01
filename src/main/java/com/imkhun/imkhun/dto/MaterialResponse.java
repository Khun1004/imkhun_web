package com.imkhun.imkhun.dto;

import java.util.List;

// level은 한국어 KWZM 자료에서만 값이 있음 ("BEGINNER"/"LEVEL1"~"LEVEL4"), 그 외에는 null
public record MaterialResponse(
        Long id,
        String language,
        String category,
        String title,
        String description,
        List<MaterialFileResponse> files,
        String createdAt,
        List<String> assignedStudentNumbers,
        String scope,
        String level
) {
}