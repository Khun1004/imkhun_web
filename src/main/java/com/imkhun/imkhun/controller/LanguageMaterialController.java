package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.dto.LanguageMaterialResponse;
import com.imkhun.imkhun.service.LanguageMaterialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// imkhun 공개 홈페이지 "강의 자료" 탭 — 로그인 안 해도 누구나 볼 수 있음
@RestController
@RequestMapping("/api/language-materials")
public class LanguageMaterialController {

    private final LanguageMaterialService languageMaterialService;

    public LanguageMaterialController(LanguageMaterialService languageMaterialService) {
        this.languageMaterialService = languageMaterialService;
    }

    @GetMapping
    public ResponseEntity<List<LanguageMaterialResponse>> getLanguageMaterials() {
        return ResponseEntity.ok(languageMaterialService.getAllMaterials());
    }
}