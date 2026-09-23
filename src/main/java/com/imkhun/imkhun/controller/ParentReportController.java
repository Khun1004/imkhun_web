package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.service.ParentReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 부모님이 로그인 없이 링크(토큰)만으로 들어와서 보는 리포트 — 공개 엔드포인트
@RestController
@RequestMapping("/api/parent-report")
public class ParentReportController {

    private final ParentReportService parentReportService;

    public ParentReportController(ParentReportService parentReportService) {
        this.parentReportService = parentReportService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> getReport(@PathVariable String token) {
        try {
            return ResponseEntity.ok(parentReportService.getReportByToken(token));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}