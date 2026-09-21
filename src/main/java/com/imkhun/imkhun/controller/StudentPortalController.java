package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.*;
import com.imkhun.imkhun.service.AdminFileService;
import com.imkhun.imkhun.service.ApplicationService;
import com.imkhun.imkhun.service.AssignmentService;
import com.imkhun.imkhun.service.AssignmentSubmissionService;
import com.imkhun.imkhun.service.LearningGoalService;
import com.imkhun.imkhun.service.AttendanceService;
import com.imkhun.imkhun.service.AttendanceStreakService;
import com.imkhun.imkhun.service.CalendarExportService;
import com.imkhun.imkhun.service.ClassChangeRequestService;
import com.imkhun.imkhun.service.GrowthReportService;
import com.imkhun.imkhun.service.KwzmInviteService;
import com.imkhun.imkhun.service.NotificationService;
import com.imkhun.imkhun.service.StudentAuthService;
import com.imkhun.imkhun.service.StudentDashboardService;
import com.imkhun.imkhun.service.StudentCalendarService;
import com.imkhun.imkhun.service.StudyMaterialService;
import com.imkhun.imkhun.service.StudyPostService;
import com.imkhun.imkhun.service.SurveyService;
import com.imkhun.imkhun.service.VocabularyService;
import com.imkhun.imkhun.service.VoiceSubmissionService;
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
    private final AdminFileService adminFileService;
    private final AttendanceService attendanceService;
    private final AssignmentService assignmentService;
    private final ClassChangeRequestService classChangeRequestService;
    private final SurveyService surveyService;
    private final CalendarExportService calendarExportService;
    private final AttendanceStreakService attendanceStreakService;
    private final VocabularyService vocabularyService;
    private final GrowthReportService growthReportService;
    private final VoiceSubmissionService voiceSubmissionService;
    private final StudentDashboardService studentDashboardService;
    private final StudentCalendarService studentCalendarService;
    private final AssignmentSubmissionService assignmentSubmissionService;
    private final LearningGoalService learningGoalService;

    public StudentPortalController(StudentAuthService studentAuthService, ApplicationService applicationService,
                                   StudyMaterialService studyMaterialService, KwzmInviteService kwzmInviteService,
                                   StudyPostService studyPostService, NotificationService notificationService,
                                   AdminFileService adminFileService, AttendanceService attendanceService,
                                   AssignmentService assignmentService, ClassChangeRequestService classChangeRequestService,
                                   SurveyService surveyService, CalendarExportService calendarExportService,
                                   AttendanceStreakService attendanceStreakService, VocabularyService vocabularyService,
                                   GrowthReportService growthReportService, VoiceSubmissionService voiceSubmissionService,
                                   StudentDashboardService studentDashboardService, StudentCalendarService studentCalendarService,
                                   AssignmentSubmissionService assignmentSubmissionService, LearningGoalService learningGoalService) {
        this.studentAuthService = studentAuthService;
        this.applicationService = applicationService;
        this.notificationService = notificationService;
        this.adminFileService = adminFileService;
        this.attendanceService = attendanceService;
        this.assignmentService = assignmentService;
        this.classChangeRequestService = classChangeRequestService;
        this.surveyService = surveyService;
        this.calendarExportService = calendarExportService;
        this.attendanceStreakService = attendanceStreakService;
        this.vocabularyService = vocabularyService;
        this.growthReportService = growthReportService;
        this.voiceSubmissionService = voiceSubmissionService;
        this.studentDashboardService = studentDashboardService;
        this.studentCalendarService = studentCalendarService;
        this.assignmentSubmissionService = assignmentSubmissionService;
        this.learningGoalService = learningGoalService;
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

        // 승인됐어도, 초대(자료 또는 영상)가 하나도 없으면 "내 수강 정보"에서도 빠짐 —
        // 관리자가 초대를 지웠으면 학생 화면에서도 그 강의가 안 보이는 게 자연스러워서요.
        List<StudentCourseResponse> courses = studentAuthService.getApprovedApplications(user.getUsername())
                .stream()
                .filter(a -> {
                    String lang = applicationService.extractLanguageCode(a.getCourseName());
                    return isInvitedForLanguage(user.getUsername(), lang, "MATERIAL")
                            || isInvitedForLanguage(user.getUsername(), lang, "VIDEO");
                })
                .map(a -> new StudentCourseResponse(
                        a.getId(), a.getStudentNumber(), a.getCourseName(), applicationService.extractLanguageCode(a.getCourseName())
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

    // "내 수강 정보" 카드를 눌렀을 때 — 그 강의(언어)의 자료를 항목 구분 없이 한 번에 다 보여줘요
    @GetMapping("/materials/by-course")
    public ResponseEntity<?> getMaterialsByCourse(HttpServletRequest request, @RequestParam String language) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        User user = userOpt.get();

        if (!isInvitedForLanguage(user.getUsername(), language, "MATERIAL")) {
            return ResponseEntity.status(403).body("아직 이 언어 자료를 볼 수 있게 초대받지 못했어요. 선생님께 문의해주세요.");
        }

        return ResponseEntity.ok(studyMaterialService.getAllMaterialsForLanguage(language, "KWZM"));
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

    // 검색 — 자료(내가 초대받은 언어만) + 게시판 글, 제목/내용에 검색어가 들어간 것들을 같이 보여줘요
    @GetMapping("/search")
    public ResponseEntity<?> search(HttpServletRequest request, @RequestParam String q) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        User user = userOpt.get();

        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(new SearchResultResponse(List.of(), List.of()));
        }

        List<Application> approved = studentAuthService.getApprovedApplications(user.getUsername());
        Set<String> invitedLanguages = approved.stream()
                .map(a -> applicationService.extractLanguageCode(a.getCourseName()))
                .filter(lang -> isInvitedForLanguage(user.getUsername(), lang, "MATERIAL"))
                .collect(java.util.stream.Collectors.toSet());

        List<MaterialResponse> materials = studyMaterialService.searchMaterials(q, "KWZM").stream()
                .filter(m -> invitedLanguages.contains(m.language()))
                .toList();
        List<PostResponse> posts = studyPostService.searchPosts(q, user.getUsername());

        return ResponseEntity.ok(new SearchResultResponse(materials, posts));
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

    // 관리자가 보내준 파일 (자격증, 시험 자료) — 마이페이지 "바로가기"에서 확인
    @GetMapping("/files")
    public ResponseEntity<?> getMyFiles(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(adminFileService.getFilesForStudent(userOpt.get().getUsername()));
    }

    // 지금 출석 체크할 수 있는 강의가 있는지 (홈 화면에 "출석하기" 버튼을 보여줄지 판단용)
    @GetMapping("/attendance/check-in-options")
    public ResponseEntity<?> getCheckInOptions(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(attendanceService.getCheckinStatusForStudent(userOpt.get().getUsername()));
    }

    // 내 전체 시간표 (요일 상관없이 등록된 모든 강의) — 출석 체크 패널에 같이 보여줌
    @GetMapping("/attendance/my-schedule")
    public ResponseEntity<?> getMySchedule(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(attendanceService.getWeeklyScheduleForStudent(userOpt.get().getUsername()));
    }

    // 학생 스스로 출석 체크
    @PostMapping("/applications/{id}/check-in")
    public ResponseEntity<?> checkIn(HttpServletRequest request, @PathVariable Long id) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        try {
            String status = attendanceService.checkInSelf(id, userOpt.get().getUsername());
            return ResponseEntity.ok(java.util.Map.of("status", status));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- 숙제 / 과제 ----------

    // 내 모든 강의에 걸친 숙제를 한번에 모아서 보여줌
    @GetMapping("/assignments")
    public ResponseEntity<?> getMyAssignments(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(assignmentService.getForStudent(userOpt.get().getUsername()));
    }

    @PostMapping("/assignments/{id}/complete")
    public ResponseEntity<?> completeAssignment(HttpServletRequest request, @PathVariable Long id) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        try {
            assignmentService.toggleComplete(id, userOpt.get().getUsername(), true);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/assignments/{id}/incomplete")
    public ResponseEntity<?> uncompleteAssignment(HttpServletRequest request, @PathVariable Long id) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        try {
            assignmentService.toggleComplete(id, userOpt.get().getUsername(), false);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- 수업 취소/변경 요청 ----------

    // 이 강의(신청)에 대해 취소/변경 요청을 남김 — 본인 신청인지는 서비스에서 확인함
    @PostMapping("/applications/{id}/class-change-requests")
    public ResponseEntity<?> createClassChangeRequest(HttpServletRequest request, @PathVariable Long id,
                                                      @RequestBody CreateClassChangeRequest changeRequest) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        try {
            return ResponseEntity.ok(classChangeRequestService.createRequest(id, userOpt.get().getUsername(), changeRequest));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 마이페이지 "수업 변경 요청" — 본인의 모든 요청을 한번에 모아서 보여줌
    @GetMapping("/class-change-requests")
    public ResponseEntity<?> getMyClassChangeRequests(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(classChangeRequestService.getForStudent(userOpt.get().getUsername()));
    }

    // "수업 변경 요청"에서 강의를 고를 때 씀 — 자료 초대 여부랑 상관없이 승인된 신청은 다 나옴
    // ("내 수강 정보" 카드는 초대까지 있어야 뜨지만, 수업 변경 요청은 승인만 되어있으면 할 수 있어야 해서 따로 만듦)
    @GetMapping("/enrolled-courses")
    public ResponseEntity<?> getEnrolledCourses(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        var courses = studentAuthService.getApprovedApplications(userOpt.get().getUsername()).stream()
                .filter(a -> !"TRIAL".equals(a.getStudyType())) // 무료체험은 취소/변경 요청 대상이 아니라서 뺌
                .map(a -> new EnrolledCourseResponse(a.getId(), a.getCourseName(), applicationService.extractLanguageCode(a.getCourseName())))
                .toList();
        return ResponseEntity.ok(courses);
    }

    // ---------- 만족도 설문 ----------

    @PostMapping("/applications/{id}/survey")
    public ResponseEntity<?> submitSurvey(HttpServletRequest request, @PathVariable Long id,
                                          @RequestBody CreateSurveyRequest surveyRequest) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        try {
            surveyService.submitSurvey(id, userOpt.get().getUsername(), surveyRequest);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/applications/{id}/survey/status")
    public ResponseEntity<?> getSurveyStatus(HttpServletRequest request, @PathVariable Long id) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(java.util.Map.of("submitted", surveyService.hasSubmitted(id)));
    }

    // ---------- 구글 캘린더 내보내기 ----------

    // 수업 요일/시간이 설정된 강의들을 .ics 파일로 만들어서 다운로드시킴 — 구글/애플/아웃룩 캘린더에 가져오기(import) 가능
    @GetMapping(value = "/calendar.ics", produces = "text/calendar;charset=UTF-8")
    public ResponseEntity<byte[]> exportCalendar(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).build();

        var applications = studentAuthService.getApprovedApplications(userOpt.get().getUsername());
        String ics = calendarExportService.buildIcsForApplications(applications);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"imkhun_schedule.ics\"")
                .body(ics.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // ---------- 출석 스트릭 / 배지 ----------

    @GetMapping("/attendance/streak")
    public ResponseEntity<?> getAttendanceStreak(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(attendanceStreakService.getStreakForStudent(userOpt.get().getUsername()));
    }

    // ---------- 단어장 / 플래시카드 ----------

    @GetMapping("/vocabulary/sets/{language}")
    public ResponseEntity<?> getVocabularySetsForLanguage(HttpServletRequest request, @PathVariable String language) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(vocabularyService.getSetsForStudent(language));
    }

    @GetMapping("/vocabulary/sets/{setId}/words")
    public ResponseEntity<?> getFlashcards(HttpServletRequest request, @PathVariable Long setId) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(vocabularyService.getFlashcards(setId));
    }

    @GetMapping("/vocabulary/sets/{setId}/quiz-result")
    public ResponseEntity<?> getBestQuizResult(HttpServletRequest request, @PathVariable Long setId) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(vocabularyService.getBestQuizResult(setId, userOpt.get().getUsername()));
    }

    @PostMapping("/vocabulary/sets/{setId}/quiz-result")
    public ResponseEntity<?> submitQuizResult(HttpServletRequest request, @PathVariable Long setId,
                                              @RequestBody SubmitQuizResultRequest resultRequest) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        try {
            vocabularyService.submitQuizResult(setId, userOpt.get().getUsername(), resultRequest);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- 나의 성장 리포트 ----------

    @GetMapping("/growth-report")
    public ResponseEntity<?> getGrowthReport(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(growthReportService.getReport(userOpt.get().getUsername()));
    }

    // ---------- 발음/음성 녹음 제출 ----------

    @PostMapping("/voice-submissions")
    public ResponseEntity<?> submitVoiceRecording(HttpServletRequest request, @RequestBody CreateVoiceSubmissionRequest submissionRequest) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        try {
            voiceSubmissionService.submit(userOpt.get().getUsername(), submissionRequest);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/voice-submissions")
    public ResponseEntity<?> getMyVoiceSubmissions(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(voiceSubmissionService.getForStudent(userOpt.get().getUsername()));
    }

    // ---------- 오늘 할 일 요약 ----------

    @GetMapping("/today-summary")
    public ResponseEntity<?> getTodaySummary(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(studentDashboardService.getTodaySummary(userOpt.get().getUsername()));
    }

    // ---------- 달력 보기 ----------

    @GetMapping("/calendar")
    public ResponseEntity<?> getCalendarEvents(HttpServletRequest request, @RequestParam int year, @RequestParam int month) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(studentCalendarService.getMonthEvents(userOpt.get().getUsername(), year, month));
    }

    // ---------- 숙제 제출 ----------

    @PostMapping("/assignments/{id}/submit")
    public ResponseEntity<?> submitAssignmentWork(HttpServletRequest request, @PathVariable Long id,
                                                  @RequestBody CreateSubmissionRequest submissionRequest) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        try {
            assignmentSubmissionService.submit(id, userOpt.get().getUsername(), submissionRequest);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/assignments/{id}/submission")
    public ResponseEntity<?> getMyAssignmentSubmission(HttpServletRequest request, @PathVariable Long id) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(assignmentSubmissionService.getForStudent(id, userOpt.get().getUsername()));
    }

    // ---------- 학습 목표 ----------

    @GetMapping("/goals")
    public ResponseEntity<?> getMyGoals(HttpServletRequest request) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        return ResponseEntity.ok(learningGoalService.getGoalsForStudent(userOpt.get().getUsername()));
    }

    @PostMapping("/goals")
    public ResponseEntity<?> createGoal(HttpServletRequest request, @RequestBody CreateGoalRequest goalRequest) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        try {
            return ResponseEntity.ok(learningGoalService.createGoal(userOpt.get().getUsername(), goalRequest));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/goals/{id}")
    public ResponseEntity<?> deleteGoal(HttpServletRequest request, @PathVariable Long id) {
        Optional<User> userOpt = studentAuthService.getLoggedInUser(request);
        if (userOpt.isEmpty()) return ResponseEntity.status(403).body("로그인이 필요해요.");
        try {
            learningGoalService.deleteGoal(id, userOpt.get().getUsername());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}