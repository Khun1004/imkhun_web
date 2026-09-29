package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.Assignment;
import com.imkhun.imkhun.domain.SharedGoal;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.domain.VocabularyQuizResult;
import com.imkhun.imkhun.dto.CreateSharedGoalRequest;
import com.imkhun.imkhun.dto.SharedGoalResponse;
import com.imkhun.imkhun.repository.AssignmentRepository;
import com.imkhun.imkhun.repository.AttendanceRecordRepository;
import com.imkhun.imkhun.repository.SharedGoalRepository;
import com.imkhun.imkhun.repository.UserRepository;
import com.imkhun.imkhun.repository.VocabularyQuizResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

// 친구와 "같이" 도전하는 학습 목표. 초대 → 수락 → 각자 진행률을 채우다가, 둘 다 목표에 도달하면
// 같이 축하 알림을 받는 흐름. 개인 목표(LearningGoalService)와 같은 종류를 쓰지만 별도로 관리함
@Service
public class SharedGoalService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ATTENDED_STATUSES = Set.of("PRESENT", "LATE", "MAKEUP");
    private static final Set<String> VALID_TYPES = Set.of("ATTENDANCE_STREAK", "ATTENDANCE_COUNT", "VOCAB_WORDS", "ASSIGNMENT_COUNT");

    private final SharedGoalRepository sharedGoalRepository;
    private final FriendService friendService;
    private final StudentAuthService studentAuthService;
    private final UserRepository userRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceStreakService attendanceStreakService;
    private final VocabularyQuizResultRepository vocabularyQuizResultRepository;
    private final AssignmentRepository assignmentRepository;
    private final NotificationService notificationService;

    public SharedGoalService(SharedGoalRepository sharedGoalRepository, FriendService friendService,
                             StudentAuthService studentAuthService, UserRepository userRepository,
                             AttendanceRecordRepository attendanceRecordRepository, AttendanceStreakService attendanceStreakService,
                             VocabularyQuizResultRepository vocabularyQuizResultRepository, AssignmentRepository assignmentRepository,
                             NotificationService notificationService) {
        this.sharedGoalRepository = sharedGoalRepository;
        this.friendService = friendService;
        this.studentAuthService = studentAuthService;
        this.userRepository = userRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceStreakService = attendanceStreakService;
        this.vocabularyQuizResultRepository = vocabularyQuizResultRepository;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public SharedGoalResponse createSharedGoal(String username, CreateSharedGoalRequest request) {
        if (request.friendUsername() == null || request.friendUsername().isBlank()) {
            throw new IllegalStateException("같이 도전할 친구를 선택해주세요.");
        }
        if (request.friendUsername().equals(username)) {
            throw new IllegalStateException("본인과는 같이 목표를 만들 수 없어요.");
        }
        if (!friendService.isFriend(username, request.friendUsername())) {
            throw new IllegalStateException("친구 사이일 때만 같이 목표를 만들 수 있어요.");
        }
        if (request.type() == null || !VALID_TYPES.contains(request.type())) {
            throw new IllegalStateException("목표 종류를 다시 선택해주세요.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalStateException("목표 이름을 입력해주세요.");
        }
        if (request.targetValue() <= 0) {
            throw new IllegalStateException("목표 값은 1 이상이어야 해요.");
        }

        SharedGoal saved = sharedGoalRepository.save(
                SharedGoal.create(username, request.friendUsername(), request.type(), request.title(), request.targetValue()));

        String myNickname = nicknameOf(username);
        notificationService.notifyStudent(request.friendUsername(), "SHARED_GOAL_INVITE",
                myNickname + "님이 같이 목표 도전을 제안했어요: \"" + request.title() + "\"", null);

        return toResponse(saved, username);
    }

    @Transactional
    public List<SharedGoalResponse> getSharedGoalsForStudent(String username) {
        return sharedGoalRepository.findByCreatorUsernameOrPartnerUsernameOrderByCreatedAtDesc(username, username).stream()
                .map(g -> toResponse(g, username))
                .toList();
    }

    @Transactional
    public void acceptSharedGoal(Long id, String username) {
        SharedGoal goal = getForPartner(id, username);
        if (!"PENDING".equals(goal.getStatus())) {
            throw new IllegalStateException("이미 처리된 제안이에요.");
        }
        goal.accept();
        sharedGoalRepository.save(goal);
        notificationService.notifyStudent(goal.getCreatorUsername(), "SHARED_GOAL_ACCEPTED",
                nicknameOf(username) + "님이 같이 목표 도전을 수락했어요! 같이 시작해봐요 💪", null);
    }

    @Transactional
    public void declineSharedGoal(Long id, String username) {
        SharedGoal goal = getForPartner(id, username);
        if (!"PENDING".equals(goal.getStatus())) {
            throw new IllegalStateException("이미 처리된 제안이에요.");
        }
        goal.decline();
        sharedGoalRepository.save(goal);
    }

    @Transactional
    public void deleteSharedGoal(Long id, String username) {
        SharedGoal goal = sharedGoalRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("목표를 찾을 수 없어요."));
        if (!goal.getCreatorUsername().equals(username) && !goal.getPartnerUsername().equals(username)) {
            throw new IllegalStateException("이 목표를 삭제할 수 없어요.");
        }
        sharedGoalRepository.deleteById(id);
    }

    private SharedGoal getForPartner(Long id, String username) {
        SharedGoal goal = sharedGoalRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("목표를 찾을 수 없어요."));
        if (!goal.getPartnerUsername().equals(username)) {
            throw new IllegalStateException("초대받은 사람만 수락하거나 거절할 수 있어요.");
        }
        return goal;
    }

    private SharedGoalResponse toResponse(SharedGoal goal, String viewerUsername) {
        boolean viewerIsCreator = goal.getCreatorUsername().equals(viewerUsername);
        String friendUsername = viewerIsCreator ? goal.getPartnerUsername() : goal.getCreatorUsername();

        if (!"ACTIVE".equals(goal.getStatus())) {
            return new SharedGoalResponse(goal.getId(), goal.getType(), goal.getTitle(), goal.getTargetValue(), goal.getStatus(),
                    friendUsername, nicknameOf(friendUsername), 0, false, 0, false, viewerIsCreator,
                    goal.getCreatedAt().format(DATETIME_FORMAT));
        }

        int creatorProgress = calculateProgress(goal, goal.getCreatorUsername());
        int partnerProgress = calculateProgress(goal, goal.getPartnerUsername());
        boolean bothWereAchieved = goal.getCreatorAchievedAt() != null && goal.getPartnerAchievedAt() != null;

        if (creatorProgress >= goal.getTargetValue()) goal.markCreatorAchieved();
        if (partnerProgress >= goal.getTargetValue()) goal.markPartnerAchieved();
        sharedGoalRepository.save(goal);

        boolean bothNowAchieved = goal.getCreatorAchievedAt() != null && goal.getPartnerAchievedAt() != null;
        if (bothNowAchieved && !bothWereAchieved) {
            String creatorNickname = nicknameOf(goal.getCreatorUsername());
            String partnerNickname = nicknameOf(goal.getPartnerUsername());
            notificationService.notifyStudent(goal.getCreatorUsername(), "SHARED_GOAL_ACHIEVED",
                    partnerNickname + "님과 함께 \"" + goal.getTitle() + "\" 목표를 둘 다 달성했어요! 축하해요 🎉", null);
            notificationService.notifyStudent(goal.getPartnerUsername(), "SHARED_GOAL_ACHIEVED",
                    creatorNickname + "님과 함께 \"" + goal.getTitle() + "\" 목표를 둘 다 달성했어요! 축하해요 🎉", null);
        }

        int myProgress = viewerIsCreator ? creatorProgress : partnerProgress;
        boolean myAchieved = viewerIsCreator ? goal.getCreatorAchievedAt() != null : goal.getPartnerAchievedAt() != null;
        int friendProgress = viewerIsCreator ? partnerProgress : creatorProgress;
        boolean friendAchieved = viewerIsCreator ? goal.getPartnerAchievedAt() != null : goal.getCreatorAchievedAt() != null;

        return new SharedGoalResponse(goal.getId(), goal.getType(), goal.getTitle(), goal.getTargetValue(), goal.getStatus(),
                friendUsername, nicknameOf(friendUsername),
                Math.min(myProgress, goal.getTargetValue()), myAchieved,
                Math.min(friendProgress, goal.getTargetValue()), friendAchieved,
                viewerIsCreator, goal.getCreatedAt().format(DATETIME_FORMAT));
    }

    private int calculateProgress(SharedGoal goal, String username) {
        LocalDateTime since = goal.getAcceptedAt() != null ? goal.getAcceptedAt() : goal.getCreatedAt();
        return switch (goal.getType()) {
            case "ATTENDANCE_STREAK" -> attendanceStreakService.getStreakForStudent(username).currentStreak();
            case "ATTENDANCE_COUNT" -> countAttendanceSince(username, since);
            case "VOCAB_WORDS" -> sumVocabScoresSince(username, since);
            case "ASSIGNMENT_COUNT" -> countCompletedAssignmentsSince(username, since);
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

    private String nicknameOf(String username) {
        return userRepository.findByUsername(username).map(User::getNickname).orElse("(알 수 없음)");
    }
}