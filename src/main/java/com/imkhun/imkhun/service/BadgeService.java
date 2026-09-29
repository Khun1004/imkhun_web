package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.Assignment;
import com.imkhun.imkhun.domain.AttendanceRecord;
import com.imkhun.imkhun.domain.LearningGoal;
import com.imkhun.imkhun.domain.StudentBadge;
import com.imkhun.imkhun.domain.VocabularyQuizResult;
import com.imkhun.imkhun.dto.BadgeResponse;
import com.imkhun.imkhun.repository.AssignmentRepository;
import com.imkhun.imkhun.repository.AttendanceRecordRepository;
import com.imkhun.imkhun.repository.LearningGoalRepository;
import com.imkhun.imkhun.repository.StudentBadgeRepository;
import com.imkhun.imkhun.repository.VocabularyQuizResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 학생이 출석/단어/숙제/커뮤니티 활동을 하면서 모으는 배지(업적) 시스템.
// 한 번 획득한 배지는 계속 유지되고, 새로 획득한 순간에 학생에게 알림을 보내줌.
@Service
public class BadgeService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ATTENDED_STATUSES = Set.of("PRESENT", "LATE", "MAKEUP");

    // 통계 종류 (statKey)
    private static final String STAT_STREAK = "attendanceStreak";
    private static final String STAT_TOTAL_ATTEND = "totalAttendance";
    private static final String STAT_VOCAB = "vocabWords";
    private static final String STAT_ASSIGNMENT = "assignmentsCompleted";
    private static final String STAT_POST = "postsWritten";
    private static final String STAT_VOICE = "voiceSubmissions";
    private static final String STAT_GOAL = "goalsAchieved";

    private record BadgeDefinition(String key, String statKey, int threshold, String name, String description,
                                   String icon, String category) {
    }

    private static final List<BadgeDefinition> CATALOG = List.of(
            new BadgeDefinition("ATTEND_STREAK_3", STAT_STREAK, 3, "꾸준한 시작", "3일 연속으로 출석했어요", "🔥", "출석"),
            new BadgeDefinition("ATTEND_STREAK_7", STAT_STREAK, 7, "일주일 개근", "7일 연속으로 출석했어요", "🌟", "출석"),
            new BadgeDefinition("ATTEND_STREAK_30", STAT_STREAK, 30, "한 달의 노력", "30일 연속으로 출석했어요", "🏆", "출석"),
            new BadgeDefinition("ATTEND_TOTAL_10", STAT_TOTAL_ATTEND, 10, "성실한 학생", "총 10번 출석했어요", "📘", "출석"),
            new BadgeDefinition("ATTEND_TOTAL_50", STAT_TOTAL_ATTEND, 50, "출석 베테랑", "총 50번 출석했어요", "🎖️", "출석"),
            new BadgeDefinition("ATTEND_TOTAL_100", STAT_TOTAL_ATTEND, 100, "출석의 달인", "총 100번 출석했어요", "💎", "출석"),
            new BadgeDefinition("VOCAB_50", STAT_VOCAB, 50, "단어 새싹", "단어 점수를 50점 모았어요", "🌱", "단어장"),
            new BadgeDefinition("VOCAB_200", STAT_VOCAB, 200, "단어왕", "단어 점수를 200점 모았어요", "👑", "단어장"),
            new BadgeDefinition("ASSIGN_5", STAT_ASSIGNMENT, 5, "숙제 척척박사", "숙제를 5개 완료했어요", "✏️", "숙제"),
            new BadgeDefinition("ASSIGN_20", STAT_ASSIGNMENT, 20, "숙제왕", "숙제를 20개 완료했어요", "🎯", "숙제"),
            new BadgeDefinition("POST_5", STAT_POST, 5, "이야기꾼", "게시판에 글을 5개 남겼어요", "💬", "커뮤니티"),
            new BadgeDefinition("VOICE_5", STAT_VOICE, 5, "발음 연습생", "발음 녹음을 5번 제출했어요", "🎤", "커뮤니티"),
            new BadgeDefinition("GOAL_1", STAT_GOAL, 1, "목표 달성", "학습 목표를 1개 달성했어요", "✅", "목표"),
            new BadgeDefinition("GOAL_5", STAT_GOAL, 5, "목표 헌터", "학습 목표를 5개 달성했어요", "🥇", "목표")
    );

    private final StudentBadgeRepository studentBadgeRepository;
    private final StudentAuthService studentAuthService;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceStreakService attendanceStreakService;
    private final VocabularyQuizResultRepository vocabularyQuizResultRepository;
    private final AssignmentRepository assignmentRepository;
    private final LearningGoalRepository learningGoalRepository;
    private final StudyPostService studyPostService;
    private final VoiceSubmissionService voiceSubmissionService;
    private final NotificationService notificationService;

    public BadgeService(StudentBadgeRepository studentBadgeRepository, StudentAuthService studentAuthService,
                        AttendanceRecordRepository attendanceRecordRepository, AttendanceStreakService attendanceStreakService,
                        VocabularyQuizResultRepository vocabularyQuizResultRepository, AssignmentRepository assignmentRepository,
                        LearningGoalRepository learningGoalRepository, StudyPostService studyPostService,
                        VoiceSubmissionService voiceSubmissionService, NotificationService notificationService) {
        this.studentBadgeRepository = studentBadgeRepository;
        this.studentAuthService = studentAuthService;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceStreakService = attendanceStreakService;
        this.vocabularyQuizResultRepository = vocabularyQuizResultRepository;
        this.assignmentRepository = assignmentRepository;
        this.learningGoalRepository = learningGoalRepository;
        this.studyPostService = studyPostService;
        this.voiceSubmissionService = voiceSubmissionService;
        this.notificationService = notificationService;
    }

    @Transactional
    public List<BadgeResponse> getBadgesForStudent(String username) {
        Map<String, Integer> stats = computeStats(username);
        Map<String, StudentBadge> earnedByKey = new HashMap<>();
        studentBadgeRepository.findByUsername(username).forEach(b -> earnedByKey.put(b.getBadgeKey(), b));

        return CATALOG.stream()
                .map(def -> {
                    int currentValue = stats.getOrDefault(def.statKey(), 0);
                    boolean nowQualifies = currentValue >= def.threshold();
                    StudentBadge existing = earnedByKey.get(def.key());

                    if (nowQualifies && existing == null) {
                        existing = studentBadgeRepository.save(StudentBadge.create(username, def.key()));
                        notificationService.notifyStudent(username, "BADGE_EARNED",
                                "\"" + def.name() + "\" 배지를 획득했어요! 축하해요 🎉", null);
                    }

                    boolean earned = existing != null;
                    return new BadgeResponse(
                            def.key(), def.name(), def.description(), def.icon(), def.category(),
                            earned, earned ? existing.getEarnedAt().format(DATETIME_FORMAT) : null,
                            Math.min(currentValue, def.threshold()), def.threshold()
                    );
                })
                .toList();
    }

    private Map<String, Integer> computeStats(String username) {
        Map<String, Integer> stats = new HashMap<>();
        List<Application> applications = studentAuthService.getApprovedApplications(username);
        List<Long> applicationIds = applications.stream().map(Application::getId).toList();

        stats.put(STAT_STREAK, attendanceStreakService.getStreakForStudent(username).currentStreak());

        int totalAttendance = 0;
        for (Long applicationId : applicationIds) {
            totalAttendance += (int) attendanceRecordRepository.findByApplicationIdOrderByClassDateDesc(applicationId).stream()
                    .filter(r -> ATTENDED_STATUSES.contains(r.getStatus()))
                    .count();
        }
        stats.put(STAT_TOTAL_ATTEND, totalAttendance);

        int vocabWords = vocabularyQuizResultRepository.findByUsername(username).stream()
                .mapToInt(VocabularyQuizResult::getScore)
                .sum();
        stats.put(STAT_VOCAB, vocabWords);

        int assignmentsCompleted = (int) assignmentRepository.findByApplicationIdInOrderByCompletedAscDueDateAsc(applicationIds).stream()
                .filter(Assignment::isCompleted)
                .count();
        stats.put(STAT_ASSIGNMENT, assignmentsCompleted);

        stats.put(STAT_POST, studyPostService.getMyPosts(username).size());
        stats.put(STAT_VOICE, voiceSubmissionService.getForStudent(username).size());

        int goalsAchieved = (int) learningGoalRepository.findByUsernameOrderByCreatedAtDesc(username).stream()
                .filter(g -> g.getAchievedAt() != null)
                .count();
        stats.put(STAT_GOAL, goalsAchieved);

        return stats;
    }
}