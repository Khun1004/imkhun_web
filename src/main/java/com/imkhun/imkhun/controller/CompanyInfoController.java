package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.service.CompanyInfoService;
import com.imkhun.imkhun.service.FuturePlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 회사소개/사업소개/미래 계획 — 로그인 안 해도 누구나 볼 수 있음 (imkhun 공개 사이트, KWZM Center 둘 다 이 API를 그대로 씀)
@RestController
@RequestMapping("/api")
public class CompanyInfoController {

    private final CompanyInfoService companyInfoService;
    private final FuturePlanService futurePlanService;

    public CompanyInfoController(CompanyInfoService companyInfoService, FuturePlanService futurePlanService) {
        this.companyInfoService = companyInfoService;
        this.futurePlanService = futurePlanService;
    }

    @GetMapping("/company-info/{type}")
    public ResponseEntity<?> getCompanyInfo(@PathVariable String type) {
        return ResponseEntity.ok(companyInfoService.getByType(type.toUpperCase()));
    }

    @GetMapping("/future-plan")
    public ResponseEntity<?> getFuturePlan() {
        return ResponseEntity.ok(futurePlanService.getFuturePlan());
    }
}