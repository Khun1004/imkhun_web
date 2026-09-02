package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.*;
import com.imkhun.imkhun.service.ApplicationService;
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

    public StudentPortalController(StudentAuthService studentAuthService, ApplicationService applicationService,
                                   StudyMaterialService studyMaterialService) {
        this.studentAuthService = studentAuthService;
        this.applicationService = applicationService;
        this.studyMaterialService = studyMaterialService;
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

    // 이 학생이 승인받은 언어의 자료만 볼 수 있음 (다른 언어 자료는 접근 불가)
    // + 특정 학생만 보도록 지정된 자료는 그 학생에게만 보임
    @GetMapping("/materials")
    public ResponseEntity<?> getMaterials(HttpServletRequest request,
                                          @RequestParam String language, @RequestParam String category) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        User user = userOpt.get();

        List<Application> approved = studentAuthService.getApprovedApplications(user.getUsername());
        Set<String> allowedLanguages = approved.stream()
                .map(a -> applicationService.extractLanguageCode(a.getCourseName()))
                .collect(java.util.stream.Collectors.toSet());

        if (!allowedLanguages.contains(language)) {
            return ResponseEntity.status(403).body("이 자료에 접근할 수 없어요.");
        }

        Set<String> myStudentNumbers = approved.stream()
                .map(Application::getStudentNumber)
                .collect(java.util.stream.Collectors.toSet());

        List<MaterialResponse> materials = studyMaterialService.getMaterials(language, category).stream()
                .filter(m -> isVisibleToStudent(m, myStudentNumbers))
                .toList();

        return ResponseEntity.ok(materials);
    }

    // 지정된 학생이 없으면(전체 공개) 누구나 보임, 지정돼있으면 내 학생번호가 포함될 때만 보임
    private boolean isVisibleToStudent(MaterialResponse material, Set<String> myStudentNumbers) {
        List<String> assigned = material.assignedStudentNumbers();
        if (assigned == null || assigned.isEmpty()) return true;
        return assigned.stream().anyMatch(myStudentNumbers::contains);
    }

    // 홈 화면 "최근 등록된 자료" — 승인받은 언어들 중 최근 6개
    @GetMapping("/materials/recent")
    public ResponseEntity<?> getRecentMaterials(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        User user = userOpt.get();

        List<Application> approved = studentAuthService.getApprovedApplications(user.getUsername());
        Set<String> allowedLanguages = approved.stream()
                .map(a -> applicationService.extractLanguageCode(a.getCourseName()))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> myStudentNumbers = approved.stream()
                .map(Application::getStudentNumber)
                .collect(java.util.stream.Collectors.toSet());

        // 넉넉히 가져온 다음 개별 지정 자료를 걸러내고 6개로 자름
        List<MaterialResponse> materials = studyMaterialService.getRecentMaterials(allowedLanguages, 30).stream()
                .filter(m -> isVisibleToStudent(m, myStudentNumbers))
                .limit(6)
                .toList();

        return ResponseEntity.ok(materials);
    }
}