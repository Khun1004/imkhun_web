package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.domain.Admin;
import com.imkhun.imkhun.dto.*;
import com.imkhun.imkhun.repository.AdminRepository;
import com.imkhun.imkhun.service.AdminAuthService;
import com.imkhun.imkhun.service.ApplicationService;
import com.imkhun.imkhun.service.KwzmInviteService;
import com.imkhun.imkhun.service.ReviewService;
import com.imkhun.imkhun.service.StudyMaterialService;
import com.imkhun.imkhun.service.StudyNoteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAuthService adminAuthService;
    private final AdminRepository adminRepository;
    private final StudyNoteService studyNoteService;
    private final ApplicationService applicationService;
    private final StudyMaterialService studyMaterialService;
    private final ReviewService reviewService;
    private final KwzmInviteService kwzmInviteService;

    public AdminController(AdminAuthService adminAuthService, AdminRepository adminRepository,
                           StudyNoteService studyNoteService, ApplicationService applicationService,
                           StudyMaterialService studyMaterialService, ReviewService reviewService,
                           KwzmInviteService kwzmInviteService) {
        this.adminAuthService = adminAuthService;
        this.adminRepository = adminRepository;
        this.studyNoteService = studyNoteService;
        this.applicationService = applicationService;
        this.studyMaterialService = studyMaterialService;
        this.reviewService = reviewService;
        this.kwzmInviteService = kwzmInviteService;
    }

    // 최초 관리자 계정 등록 (딱 한 번만 성공함 — 이미 관리자가 있으면 실패)
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody AdminSignupRequest request) {
        try {
            adminAuthService.signup(request.username(), request.password(), request.email(), request.phone());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // "나만의 공부 화면" 전용 로그인 — users 테이블/스프링시큐리티 일반 로그인과 완전 별개
    @PostMapping("/login")
    public ResponseEntity<?> login(HttpServletResponse response, @RequestBody AdminLoginRequest loginRequest) {
        boolean success = adminAuthService.login(
                response, loginRequest.username(), loginRequest.password(),
                loginRequest.email(), loginRequest.phone()
        );
        if (!success) {
            return ResponseEntity.status(401).body("아이디, 비밀번호, 이메일, 전화번호를 다시 확인해주세요.");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        adminAuthService.logout(request, response);
        return ResponseEntity.ok().build();
    }

    // 지금 이 세션이 관리자로 로그인되어 있는지 확인
    @GetMapping("/check")
    public ResponseEntity<AdminCheckResponse> checkAdmin(HttpServletRequest request) {
        return ResponseEntity.ok(new AdminCheckResponse(adminAuthService.isLoggedIn(request)));
    }

    private boolean notAdmin(HttpServletRequest request) {
        return !adminAuthService.isLoggedIn(request);
    }

    // ---------- 마이 (내 정보 / 결제 수단) ----------

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");

        String username = adminAuthService.getLoggedInUsername(request);
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isEmpty()) return ResponseEntity.status(404).build();

        Admin admin = adminOpt.get();
        return ResponseEntity.ok(new AdminMeResponse(
                admin.getUsername(), admin.getEmail(), admin.getPhone(), admin.getPaymentInfo()
        ));
    }

    @PostMapping("/payment")
    public ResponseEntity<?> updatePayment(HttpServletRequest request, @RequestBody UpdatePaymentInfoRequest paymentRequest) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");

        String username = adminAuthService.getLoggedInUsername(request);
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("관리자 정보를 찾을 수 없어요."));
        admin.updatePaymentInfo(paymentRequest.paymentInfo());
        adminRepository.save(admin);
        return ResponseEntity.ok().build();
    }

    // ---------- 학습 노트 (한국어/일본어/태국어/영어) ----------

    @GetMapping("/notes/{language}")
    public ResponseEntity<?> getNote(HttpServletRequest request, @PathVariable String language) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        try {
            return ResponseEntity.ok(studyNoteService.getNote(language));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/notes/{language}")
    public ResponseEntity<?> saveNote(HttpServletRequest request, @PathVariable String language,
                                      @RequestBody SaveNoteRequest saveRequest) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        try {
            return ResponseEntity.ok(studyNoteService.saveNote(language, saveRequest.content()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- 학생 관리 ----------

    @GetMapping("/applications")
    public ResponseEntity<?> getAllApplications(HttpServletRequest request) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        return ResponseEntity.ok(applicationService.getAllApplicationsForAdmin());
    }

    @PostMapping("/applications/{id}/status")
    public ResponseEntity<?> updateApplicationStatus(HttpServletRequest request, @PathVariable Long id,
                                                     @RequestBody UpdateStatusRequest statusRequest) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        try {
            applicationService.updateStatus(id, statusRequest.status());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 학생들이 남긴 리뷰 전체 목록 (답글 달기용)
    @GetMapping("/reviews")
    public ResponseEntity<?> getAllReviews(HttpServletRequest request) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    // 리뷰에 답글 남기기 (이미 답글이 있으면 덮어씀 = 수정)
    @PostMapping("/reviews/{id}/reply")
    public ResponseEntity<?> replyToReview(HttpServletRequest request, @PathVariable Long id,
                                           @RequestBody ReplyRequest replyRequest) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        try {
            return ResponseEntity.ok(reviewService.replyToReview(id, replyRequest.reply()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- 자료 등록 — "나만의 공부 화면" 전용 (개인용, PERSONAL) ----------

    @PostMapping("/materials")
    public ResponseEntity<?> createMaterial(HttpServletRequest request, @RequestBody CreateMaterialRequest materialRequest) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        try {
            return ResponseEntity.ok(studyMaterialService.createMaterial(materialRequest, "PERSONAL"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/materials")
    public ResponseEntity<?> getMaterials(HttpServletRequest request,
                                          @RequestParam String language, @RequestParam String category) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        return ResponseEntity.ok(studyMaterialService.getMaterials(language, category, "PERSONAL"));
    }

    @PutMapping("/materials/{id}")
    public ResponseEntity<?> updateMaterial(HttpServletRequest request, @PathVariable Long id,
                                            @RequestBody CreateMaterialRequest materialRequest) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        try {
            return ResponseEntity.ok(studyMaterialService.updateMaterial(id, materialRequest, "PERSONAL"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/materials/{id}")
    public ResponseEntity<?> deleteMaterial(HttpServletRequest request, @PathVariable Long id) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        try {
            studyMaterialService.deleteMaterial(id, "PERSONAL");
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- 자료 등록 — KWZM Center 학생용 (KWZM) ----------

    @PostMapping("/kwzm-materials")
    public ResponseEntity<?> createKwzmMaterial(HttpServletRequest request, @RequestBody CreateMaterialRequest materialRequest) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        try {
            return ResponseEntity.ok(studyMaterialService.createMaterial(materialRequest, "KWZM"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/kwzm-materials")
    public ResponseEntity<?> getKwzmMaterials(HttpServletRequest request,
                                              @RequestParam String language, @RequestParam String category) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        return ResponseEntity.ok(studyMaterialService.getMaterials(language, category, "KWZM"));
    }

    @PutMapping("/kwzm-materials/{id}")
    public ResponseEntity<?> updateKwzmMaterial(HttpServletRequest request, @PathVariable Long id,
                                                @RequestBody CreateMaterialRequest materialRequest) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        try {
            return ResponseEntity.ok(studyMaterialService.updateMaterial(id, materialRequest, "KWZM"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/kwzm-materials/{id}")
    public ResponseEntity<?> deleteKwzmMaterial(HttpServletRequest request, @PathVariable Long id) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        try {
            studyMaterialService.deleteMaterial(id, "KWZM");
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- KWZM 자료를 볼 수 있는 학생 초대 (언어 단위) ----------

    @GetMapping("/kwzm-invites")
    public ResponseEntity<?> getInvitedStudents(HttpServletRequest request, @RequestParam String language) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        return ResponseEntity.ok(kwzmInviteService.getInvitedStudents(language));
    }

    @PostMapping("/kwzm-invites")
    public ResponseEntity<?> inviteStudent(HttpServletRequest request, @RequestParam String language,
                                           @RequestBody InviteStudentRequest inviteRequest) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        try {
            kwzmInviteService.inviteStudent(language, inviteRequest.studentNumber());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/kwzm-invites")
    public ResponseEntity<?> removeInvitedStudent(HttpServletRequest request, @RequestParam String language,
                                                  @RequestParam String studentNumber) {
        if (notAdmin(request)) return ResponseEntity.status(403).body("관리자만 접근할 수 있어요.");
        kwzmInviteService.removeStudent(language, studentNumber);
        return ResponseEntity.ok().build();
    }
}