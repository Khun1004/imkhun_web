package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.*;
import com.imkhun.imkhun.service.ApplicationService;
import com.imkhun.imkhun.service.KwzmInviteService;
import com.imkhun.imkhun.service.StudentAuthService;
import com.imkhun.imkhun.service.StudyMaterialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

// "KWZM Computer Training & Language Center" 전용 API — 일반 로그인/관리자 로그인과 완전 별개
@RestController
@RequestMapping("/api/student")
public class StudentPortalController {

    private final StudentAuthService studentAuthService;
    private final ApplicationService applicationService;
    private final StudyMaterialService studyMaterialService;
    private final KwzmInviteService kwzmInviteService;

    public StudentPortalController(StudentAuthService studentAuthService, ApplicationService applicationService,
                                   StudyMaterialService studyMaterialService, KwzmInviteService kwzmInviteService) {
        this.studentAuthService = studentAuthService;
        this.applicationService = applicationService;
        this.studyMaterialService = studyMaterialService;
        this.kwzmInviteService = kwzmInviteService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(HttpServletResponse response, @RequestBody StudentLoginRequest loginRequest) {
        boolean success = studentAuthService.login(
                response, loginRequest.username(), loginRequest.password(),
                loginRequest.email(), loginRequest.studentNumber()
        );
        if (!success) {
            return ResponseEntity.status(401).body("학생번호, 아이디, 비밀번호, 이메일을 다시 확인해주세요.");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        studentAuthService.logout(request, response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check")
    public ResponseEntity<StudentCheckResponse> check(HttpServletRequest request) {
        return ResponseEntity.ok(new StudentCheckResponse(studentAuthService.isLoggedIn(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        User user = userOpt.get();

        List<StudentCourseResponse> courses = studentAuthService.getApprovedApplications(user.getUsername())
                .stream()
                .map(a -> new StudentCourseResponse(
                        a.getStudentNumber(), a.getCourseName(), applicationService.extractLanguageCode(a.getCourseName())
                ))
                .toList();

        return ResponseEntity.ok(new StudentMeResponse(user.getNickname(), courses));
    }

    // 이 학생이 승인받은 언어이면서, 그 언어의 KWZM 자료를 볼 수 있게 "초대"까지 받은 경우에만 자료가 보임
    @GetMapping("/materials")
    public ResponseEntity<?> getMaterials(HttpServletRequest request,
                                          @RequestParam String language, @RequestParam String category) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        User user = userOpt.get();

        if (!isInvitedForLanguage(user.getUsername(), language)) {
            return ResponseEntity.status(403).body("아직 이 언어 자료를 볼 수 있게 초대받지 못했어요. 선생님께 문의해주세요.");
        }

        return ResponseEntity.ok(studyMaterialService.getMaterials(language, category, "KWZM"));
    }

    // 이 학생의 승인된 신청서 중, 이 언어에 해당하는 학생번호가 초대 목록에 있는지 확인
    private boolean isInvitedForLanguage(String username, String language) {
        List<Application> approved = studentAuthService.getApprovedApplications(username);
        return approved.stream()
                .filter(a -> language.equals(applicationService.extractLanguageCode(a.getCourseName())))
                .anyMatch(a -> kwzmInviteService.isInvited(language, a.getStudentNumber()));
    }

    // 홈 화면 "최근 등록된 자료" — 초대받은 언어들 중 최근 6개
    @GetMapping("/materials/recent")
    public ResponseEntity<?> getRecentMaterials(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        User user = userOpt.get();

        List<Application> approved = studentAuthService.getApprovedApplications(user.getUsername());
        Set<String> invitedLanguages = approved.stream()
                .map(a -> applicationService.extractLanguageCode(a.getCourseName()))
                .filter(lang -> isInvitedForLanguage(user.getUsername(), lang))
                .collect(java.util.stream.Collectors.toSet());

        List<MaterialResponse> materials = studyMaterialService.getRecentMaterials(invitedLanguages, 6);
        return ResponseEntity.ok(materials);
    }
}