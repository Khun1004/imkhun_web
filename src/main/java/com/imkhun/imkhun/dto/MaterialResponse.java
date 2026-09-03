package com.imkhun.imkhun.dto;

import java.util.List;

public record MaterialResponse(Long id, String language, String category, String title, String description,
                               List<MaterialFileResponse> files, String createdAt,
                               List<String> assignedStudentNumbers, String scope) {
}