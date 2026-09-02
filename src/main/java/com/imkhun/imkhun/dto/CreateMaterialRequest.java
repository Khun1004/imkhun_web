package com.imkhun.imkhun.dto;

import java.util.List;

public record CreateMaterialRequest(String language, String category, String title, String description,
                                    List<MaterialFileRequest> files, List<String> assignedStudentNumbers) {
}