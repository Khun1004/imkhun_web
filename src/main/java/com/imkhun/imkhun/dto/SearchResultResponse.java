package com.imkhun.imkhun.dto;

import java.util.List;

public record SearchResultResponse(List<MaterialResponse> materials, List<PostResponse> posts) {
}