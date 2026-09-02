package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.dto.ApplicationResponse;
import com.imkhun.imkhun.dto.CreateApplicationRequest;
import com.imkhun.imkhun.service.ApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    private boolean notLoggedIn(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal());
    }

    // 강의 신청 (로그인 필요)
    @PostMapping
    public ResponseEntity<?> createApplication(Authentication authentication,
                                               @RequestBody CreateApplicationRequest request) {
        if (notLoggedIn(authentication)) {
            return ResponseEntity.status(401).body("로그인이 필요해요.");
        }
        try {
            ApplicationResponse response = applicationService.createApplication(authentication.getName(), request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 내 신청 내역 (마이페이지 - 강의 신청 내역)
    @GetMapping("/mine")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(Authentication authentication) {
        if (notLoggedIn(authentication)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(applicationService.getMyApplications(authentication.getName()));
    }
}