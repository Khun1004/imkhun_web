package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.Assignment;
import com.imkhun.imkhun.domain.LearningGoal;
import com.imkhun.imkhun.domain.VocabularyQuizResult;
import com.imkhun.imkhun.dto.CreateGoalRequest;
import com.imkhun.imkhun.dto.GoalResponse;
import com.imkhun.imkhun.repository.AssignmentRepository;
import com.imkhun.imkhun.repository.AttendanceRecordRepository;
import com.imkhun.imkhun.repository.LearningGoalRepository;
import com.imkhun.imkhun.repository.VocabularyQuizResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

// 학생이 직접 정하는 "학습 목표"를 관리하고, 이미 있는 출석/단어장/숙제 데이터로 진행률을 계산해주는 서비스
@Service
public class LearningGoalService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ATTENDED_STATUSES = Set.of("PRESENT", "LATE", "MAKEUP");
    private static final Set<String> VALID_TYPES = Set.of("ATTENDANCE_STREAK", "ATTENDANCE_COUNT", "VOCAB_WORDS", "ASSIGNMENT_COUNT");

    private final LearningGoalRepository learningGoalRepository;
    private final StudentAuthService studentAuthService;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceStreakService attendanceStreakService;
    private final VocabularyQuizResultRepository vocabularyQuizResultRepository;
    private final AssignmentRepository assignmentRepository;
    private final NotificationService notificationService;

    public LearningGoalService(LearningGoalRepository learningGoalRepository, StudentAuthService studentAuthService,
                               AttendanceRecordRepository attendanceRecordRepository, AttendanceStreakService attendanceStreakService,
                               VocabularyQuizResultRepository vocabularyQuizResultRepository, AssignmentRepository assignmentRepository,
                               NotificationService notificationService) {
        this.learningGoalRepository = learningGoalRepository;
        this.studentAuthService = studentAuthService;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceStreakService = attendanceStreakService;
        this.vocabularyQuizResultRepository = vocabularyQuizResultRepository;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public GoalResponse createGoal(String username, CreateGoalRequest request) {
        if (request.type() == null || !VALID_TYPES.contains(request.type())) {
            throw new IllegalStateException("목표 종류를 다시 선택해주세요.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalStateException("목표 이름을 입력해주세요.");
        }
        if (request.targetValue() <= 0) {
            throw new IllegalStateException("목표 값은 1 이상이어야 해요.");
        }

        LearningGoal saved = learningGoalRepository.save(
                LearningGoal.create(username, request.type(), request.title(), request.targetValue()));
        return toResponse(saved, username);
    }

    @Transactional
    public List<GoalResponse> getGoalsForStudent(String username) {
        return learningGoalRepository.findByUsernameOrderByCreatedAtDesc(username).stream()
                .map(g -> toResponse(g, username))
                .toList();
    }

    @Transactional
    public void deleteGoal(Long id, String username) {
        LearningGoal goal = learningGoalRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("목표를 찾을 수 없어요."));
        if (!goal.getUsername().equals(username)) {
            throw new IllegalStateException("본인의 목표만 삭제할 수 있어요.");
        }
        learningGoalRepository.deleteById(id);
    }

    private GoalResponse toResponse(LearningGoal goal, String username) {
        int progress = calculateProgress(goal, username);
        boolean nowAchieved = progress >= goal.getTargetValue();

        if (nowAchieved && goal.getAchievedAt() == null) {
            goal.markAchieved();
            learningGoalRepository.save(goal);
            notificationService.notifyStudent(username, "GOAL_ACHIEVED",
                    "\"" + goal.getTitle() + "\" 목표를 달성했어요! 축하해요 🎉", null);
        }

        return new GoalResponse(
                goal.getId(), goal.getType(), goal.getTitle(), goal.getTargetValue(),
                Math.min(progress, goal.getTargetValue()), goal.getAchievedAt() != null,
                goal.getAchievedAt() != null ? goal.getAchievedAt().format(DATETIME_FORMAT) : null,
                goal.getCreatedAt().format(DATETIME_FORMAT)
        );
    }

    private int calculateProgress(LearningGoal goal, String username) {
        return switch (goal.getType()) {
            case "ATTENDANCE_STREAK" -> attendanceStreakService.getStreakForStudent(username).currentStreak();
            case "ATTENDANCE_COUNT" -> countAttendanceSince(username, goal.getCreatedAt());
            case "VOCAB_WORDS" -> sumVocabScoresSince(username, goal.getCreatedAt());
            case "ASSIGNMENT_COUNT" -> countCompletedAssignmentsSince(username, goal.getCreatedAt());
            default -> 0;
        };
    }

    private int countAttendanceSince(String username, LocalDateTime since) {
        List<Application> applications = studentAuthService.getApprovedApplications(username);
        int count = 0;
        for (Application application : applications) {
            count += (int) attendanceRecordRepository.findByApplicationIdOrderByClassDateDesc(application.getId()).stream()
                    .filter(r -> ATTENDED_STATUSES.contains(r.getStatus()))
                    .filter(r -> !r.getClassDate().isBefore(since.toLocalDate()))
                    .count();
        }
        return count;
    }

    private int sumVocabScoresSince(String username, LocalDateTime since) {
        return vocabularyQuizResultRepository.findByUsername(username).stream()
                .filter(r -> !r.getCompletedAt().isBefore(since))
                .mapToInt(VocabularyQuizResult::getScore)
                .sum();
    }

    private int countCompletedAssignmentsSince(String username, LocalDateTime since) {
        List<Application> applications = studentAuthService.getApprovedApplications(username);
        List<Long> applicationIds = applications.stream().map(Application::getId).toList();
        return (int) assignmentRepository.findByApplicationIdInOrderByCompletedAscDueDateAsc(applicationIds).stream()
                .filter(Assignment::isCompleted)
                .filter(a -> a.getCompletedAt() != null && !a.getCompletedAt().isBefore(since))
                .count();
    }
}