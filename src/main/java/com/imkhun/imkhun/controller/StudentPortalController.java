package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.*;
import com.imkhun.imkhun.service.ApplicationService;
import com.imkhun.imkhun.service.KwzmInviteService;
import com.imkhun.imkhun.service.NotificationService;
import com.imkhun.imkhun.service.StudentAuthService;
import com.imkhun.imkhun.service.StudyMaterialService;
import com.imkhun.imkhun.service.StudyPostService;
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
    private final StudyPostService studyPostService;
    private final NotificationService notificationService;

    public StudentPortalController(StudentAuthService studentAuthService, ApplicationService applicationService,
                                   StudyMaterialService studyMaterialService, KwzmInviteService kwzmInviteService,
                                   StudyPostService studyPostService, NotificationService notificationService) {
        this.studentAuthService = studentAuthService;
        this.applicationService = applicationService;
        this.notificationService = notificationService;
        this.studyMaterialService = studyMaterialService;
        this.kwzmInviteService = kwzmInviteService;
        this.studyPostService = studyPostService;
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

        if (!isInvitedForLanguage(user.getUsername(), language, "MATERIAL")) {
            return ResponseEntity.status(403).body("아직 이 언어 자료를 볼 수 있게 초대받지 못했어요. 선생님께 문의해주세요.");
        }

        return ResponseEntity.ok(studyMaterialService.getMaterials(language, category, "KWZM"));
    }

    // 온라인 영상 — "언어 자료"와는 별도의 초대(type=VIDEO)로 관리해요.
    // 자료로 공부하는 학생과 영상으로 공부하는 학생이 다를 수 있어서, 완전히 분리했어요.
    // 항목(category) 구분이 없어서 늘 "VIDEO" 고정 카테고리로 저장/조회해요.
    @GetMapping("/videos")
    public ResponseEntity<?> getVideos(HttpServletRequest request, @RequestParam String topic) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        User user = userOpt.get();

        if (!isInvitedForLanguage(user.getUsername(), topic, "VIDEO")) {
            return ResponseEntity.status(403).body("아직 이 영상을 볼 수 있게 초대받지 못했어요. 선생님께 문의해주세요.");
        }

        return ResponseEntity.ok(studyMaterialService.getMaterials(topic, "VIDEO", "VIDEO"));
    }

    // 무료체험 — 수업을 듣기 전에 미리 볼 수 있는 체험용 자료라, 초대 없이 로그인한 학생이면 누구나 볼 수 있어요.
    @GetMapping("/trial")
    public ResponseEntity<?> getTrialMaterials(HttpServletRequest request, @RequestParam String topic) {
        if (studentAuthService.getLoggedInUser(request).isEmpty()) {
            return ResponseEntity.status(403).body("로그인이 필요해요.");
        }
        return ResponseEntity.ok(studyMaterialService.getMaterials(topic, "TRIAL", "TRIAL"));
    }

    // 이 학생의 승인된 신청서 중, 이 언어 + 종류(자료/영상)에 해당하는 학생번호가 초대 목록에 있는지 확인
    private boolean isInvitedForLanguage(String username, String language, String contentType) {
        List<Application> approved = studentAuthService.getApprovedApplications(username);
        return approved.stream()
                .filter(a -> language.equals(applicationService.extractLanguageCode(a.getCourseName())))
                .anyMatch(a -> kwzmInviteService.isInvited(language, contentType, a.getStudentNumber()));
    }

    // 홈 화면 "최근 등록된 자료" — 초대받은(자료) 언어들 중 최근 6개
    @GetMapping("/materials/recent")
    public ResponseEntity<?> getRecentMaterials(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        User user = userOpt.get();

        List<Application> approved = studentAuthService.getApprovedApplications(user.getUsername());
        Set<String> invitedLanguages = approved.stream()
                .map(a -> applicationService.extractLanguageCode(a.getCourseName()))
                .filter(lang -> isInvitedForLanguage(user.getUsername(), lang, "MATERIAL"))
                .collect(java.util.stream.Collectors.toSet());

        List<MaterialResponse> materials = studyMaterialService.getRecentMaterials(invitedLanguages, 6);
        return ResponseEntity.ok(materials);
    }

    // ---- 게시판: 학생들이 언어/컴퓨터 관련 글을 서로 올리고 볼 수 있는 공간 ----

    @GetMapping("/posts")
    public ResponseEntity<?> getPosts(HttpServletRequest request,
                                      @RequestParam String topic,
                                      @RequestParam(required = false) String category) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(studyPostService.getPosts(topic, category, userOpt.get().getUsername()));
    }

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(HttpServletRequest request, @RequestBody CreatePostRequest createPostRequest) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        User user = userOpt.get();

        try {
            PostResponse post = studyPostService.createPost(createPostRequest, user.getUsername(), user.getNickname());
            return ResponseEntity.ok(post);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 마이페이지 "내가 쓴 글"
    @GetMapping("/posts/mine")
    public ResponseEntity<?> getMyPosts(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(studyPostService.getMyPosts(userOpt.get().getUsername()));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<?> updatePost(HttpServletRequest request, @PathVariable Long id,
                                        @RequestBody UpdatePostRequest updatePostRequest) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");

        try {
            return ResponseEntity.ok(studyPostService.updatePost(id, updatePostRequest, userOpt.get().getUsername()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<?> deletePost(HttpServletRequest request, @PathVariable Long id) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");

        try {
            studyPostService.deletePost(id, userOpt.get().getUsername());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 좋아요/싫어요 — 같은 걸 다시 누르면 취소, 다른 걸 누르면 전환
    @PostMapping("/posts/{id}/reaction")
    public ResponseEntity<?> reactToPost(HttpServletRequest request, @PathVariable Long id,
                                         @RequestBody ReactionRequest reactionRequest) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");

        try {
            return ResponseEntity.ok(studyPostService.toggleReaction(id, reactionRequest.type(), userOpt.get().getUsername()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<?> getComments(HttpServletRequest request, @PathVariable Long id) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(studyPostService.getComments(id, userOpt.get().getUsername()));
    }

    // parentCommentId를 같이 보내면 그 댓글에 대한 답글로 달려요
    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<?> addComment(HttpServletRequest request, @PathVariable Long id,
                                        @RequestBody CommentRequest commentRequest) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        User user = userOpt.get();

        try {
            return ResponseEntity.ok(studyPostService.addComment(id, commentRequest.content(), user.getUsername(),
                    user.getNickname(), commentRequest.parentCommentId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 댓글 좋아요/싫어요 — 같은 걸 다시 누르면 취소, 다른 걸 누르면 전환
    @PostMapping("/comments/{id}/reaction")
    public ResponseEntity<?> reactToComment(HttpServletRequest request, @PathVariable Long id,
                                            @RequestBody ReactionRequest reactionRequest) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");

        try {
            return ResponseEntity.ok(studyPostService.toggleCommentReaction(id, reactionRequest.type(), userOpt.get().getUsername()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---- 알림 ----

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(notificationService.getStudentNotifications(userOpt.get().getUsername()));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<?> getUnreadNotificationCount(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(notificationService.getUnreadCountForStudent(userOpt.get().getUsername()));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationRead(HttpServletRequest request, @PathVariable Long id) {
        if (studentAuthService.getLoggedInUser(request).isEmpty()) {
            return ResponseEntity.status(403).body("로그인이 필요해요.");
        }
        notificationService.markRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<?> markAllNotificationsRead(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        notificationService.markAllReadForStudent(userOpt.get().getUsername());
        return ResponseEntity.ok().build();
    }
}