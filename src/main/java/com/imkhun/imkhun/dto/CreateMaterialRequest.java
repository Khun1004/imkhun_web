package com.imkhun.imkhun.dto;

import java.util.List;

// 자료 등록/수정 요청 — level은 한국어 KWZM 자료에서만 씀 ("BEGINNER"/"LEVEL1"~"LEVEL4"), 그 외에는 null
public record CreateMaterialRequest(
        String language,
        String category,
        String title,
        String description,
        List<MaterialFileRequest> files,
        List<String> assignedStudentNumbers,
        String level
) {
}