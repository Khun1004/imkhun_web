package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.dto.ApplicationResponse;
import com.imkhun.imkhun.dto.ConfirmPaymentRequest;
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

    // "입금했어요" 버튼 — 본인 신청 내역에만 확인할 수 있어요. 영수증 이미지는 선택이에요.
    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<?> confirmPayment(Authentication authentication, @PathVariable Long id,
                                            @RequestBody(required = false) ConfirmPaymentRequest request) {
        if (notLoggedIn(authentication)) {
            return ResponseEntity.status(401).body("로그인이 필요해요.");
        }
        try {
            String receiptImage = request != null ? request.receiptImage() : null;
            applicationService.confirmPaymentByStudent(id, authentication.getName(), receiptImage);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}