package com.imkhun.imkhun.dto;

import java.util.List;

public record StudentMeResponse(String nickname, List<StudentCourseResponse> courses) {
}