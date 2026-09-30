package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.Assignment;
import com.imkhun.imkhun.domain.StudyGroup;
import com.imkhun.imkhun.domain.StudyGroupGoal;
import com.imkhun.imkhun.domain.StudyGroupGoalAchievement;
import com.imkhun.imkhun.domain.StudyGroupMember;
import com.imkhun.imkhun.domain.StudyGroupNote;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.domain.VocabularyQuizResult;
import com.imkhun.imkhun.dto.CreateStudyGroupGoalRequest;
import com.imkhun.imkhun.dto.CreateStudyGroupRequest;
import com.imkhun.imkhun.dto.PostStudyGroupNoteRequest;
import com.imkhun.imkhun.dto.StudyGroupGoalMemberProgressResponse;
import com.imkhun.imkhun.dto.StudyGroupGoalResponse;
import com.imkhun.imkhun.dto.StudyGroupMemberResponse;
import com.imkhun.imkhun.dto.StudyGroupNoteResponse;
import com.imkhun.imkhun.dto.StudyGroupResponse;
import com.imkhun.imkhun.repository.AssignmentRepository;
import com.imkhun.imkhun.repository.AttendanceRecordRepository;
import com.imkhun.imkhun.repository.StudyGroupGoalAchievementRepository;
import com.imkhun.imkhun.repository.StudyGroupGoalRepository;
import com.imkhun.imkhun.repository.StudyGroupMemberRepository;
import com.imkhun.imkhun.repository.StudyGroupNoteRepository;
import com.imkhun.imkhun.repository.StudyGroupRepository;
import com.imkhun.imkhun.repository.UserRepository;
import com.imkhun.imkhun.repository.VocabularyQuizResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 여러 친구를 묶은 "스터디 그룹" 기능. 1:1 친구 기능(쪽지/같이 목표)을 여러 명으로 확장한 버전 —
// 그룹 안에서 노트를 공유하고, 그룹 전체가 같이 도전하는 목표를 만들 수 있음
@Service
public class StudyGroupService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ATTENDED_STATUSES = Set.of("PRESENT", "LATE", "MAKEUP");
    private static final Set<String> VALID_GOAL_TYPES = Set.of("ATTENDANCE_STREAK", "ATTENDANCE_COUNT", "VOCAB_WORDS", "ASSIGNMENT_COUNT");
    private static final int NOTE_MAX_LENGTH = 500;
    private static final int MAX_GROUP_NAME_LENGTH = 30;

    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository studyGroupMemberRepository;
    private final StudyGroupNoteRepository studyGroupNoteRepository;
    private final StudyGroupGoalRepository studyGroupGoalRepository;
    private final StudyGroupGoalAchievementRepository studyGroupGoalAchievementRepository;
    private final FriendService friendService;
    private final UserRepository userRepository;
    private final StudentAuthService studentAuthService;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceStreakService attendanceStreakService;
    private final VocabularyQuizResultRepository vocabularyQuizResultRepository;
    private final AssignmentRepository assignmentRepository;
    private final NotificationService notificationService;

    public StudyGroupService(StudyGroupRepository studyGroupRepository, StudyGroupMemberRepository studyGroupMemberRepository,
                             StudyGroupNoteRepository studyGroupNoteRepository, StudyGroupGoalRepository studyGroupGoalRepository,
                             StudyGroupGoalAchievementRepository studyGroupGoalAchievementRepository, FriendService friendService,
                             UserRepository userRepository, StudentAuthService studentAuthService,
                             AttendanceRecordRepository attendanceRecordRepository, AttendanceStreakService attendanceStreakService,
                             VocabularyQuizResultRepository vocabularyQuizResultRepository, AssignmentRepository assignmentRepository,
                             NotificationService notificationService) {
        this.studyGroupRepository = studyGroupRepository;
        this.studyGroupMemberRepository = studyGroupMemberRepository;
        this.studyGroupNoteRepository = studyGroupNoteRepository;
        this.studyGroupGoalRepository = studyGroupGoalRepository;
        this.studyGroupGoalAchievementRepository = studyGroupGoalAchievementRepository;
        this.friendService = friendService;
        this.userRepository = userRepository;
        this.studentAuthService = studentAuthService;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceStreakService = attendanceStreakService;
        this.vocabularyQuizResultRepository = vocabularyQuizResultRepository;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
    }

    // ---------- 그룹 생성/조회/초대/수락/거절/나가기/삭제 ----------

    @Transactional
    public StudyGroupResponse createGroup(String username, CreateStudyGroupRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalStateException("그룹 이름을 입력해주세요.");
        }
        String name = request.name().trim();
        if (name.length() > MAX_GROUP_NAME_LENGTH) {
            throw new IllegalStateException("그룹 이름은 " + MAX_GROUP_NAME_LENGTH + "자 이내로 적어주세요.");
        }

        Set<String> inviteUsernames = new LinkedHashSet<>(request.inviteUsernames() != null ? request.inviteUsernames() : List.of());
        inviteUsernames.remove(username);
        for (String friendUsername : inviteUsernames) {
            if (!friendService.isFriend(username, friendUsername)) {
                throw new IllegalStateException("친구가 아닌 사람은 초대할 수 없어요.");
            }
        }

        StudyGroup group = studyGroupRepository.save(StudyGroup.create(name, username));
        studyGroupMemberRepository.save(StudyGroupMember.createActive(group.getId(), username));

        String myNickname = nicknameOf(username);
        for (String friendUsername : inviteUsernames) {
            studyGroupMemberRepository.save(StudyGroupMember.createInvited(group.getId(), friendUsername));
            notificationService.notifyStudent(friendUsername, "STUDY_GROUP_INVITE",
                    myNickname + "님이 \"" + name + "\" 그룹에 초대했어요!", null);
        }

        return toResponse(group, username);
    }

    @Transactional
    public List<StudyGroupResponse> getMyGroups(String username) {
        List<Long> groupIds = studyGroupMemberRepository.findByUsername(username).stream()
                .filter(m -> !"DECLINED".equals(m.getStatus()))
                .map(StudyGroupMember::getGroupId)
                .distinct()
                .toList();
        return studyGroupRepository.findAllById(groupIds).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(g -> toResponse(g, username))
                .toList();
    }

    @Transactional
    public void inviteMember(Long groupId, String username, String friendUsername) {
        requireMembership(groupId, username, "ACTIVE");
        if (friendUsername == null || friendUsername.isBlank()) {
            throw new IllegalStateException("초대할 친구를 선택해주세요.");
        }
        if (!friendService.isFriend(username, friendUsername)) {
            throw new IllegalStateException("친구가 아닌 사람은 초대할 수 없어요.");
        }

        StudyGroup group = getGroupOrThrow(groupId);
        StudyGroupMember existing = studyGroupMemberRepository.findByGroupIdAndUsername(groupId, friendUsername).orElse(null);
        if (existing != null && ("INVITED".equals(existing.getStatus()) || "ACTIVE".equals(existing.getStatus()))) {
            throw new IllegalStateException("이미 초대했거나 참여 중인 멤버예요.");
        }

        if (existing != null) {
            existing.resetToInvited();
            studyGroupMemberRepository.save(existing);
        } else {
            studyGroupMemberRepository.save(StudyGroupMember.createInvited(groupId, friendUsername));
        }

        notificationService.notifyStudent(friendUsername, "STUDY_GROUP_INVITE",
                nicknameOf(username) + "님이 \"" + group.getName() + "\" 그룹에 초대했어요!", null);
    }

    @Transactional
    public void acceptInvite(Long groupId, String username) {
        StudyGroupMember member = requireMembership(groupId, username, "INVITED");
        member.accept();
        studyGroupMemberRepository.save(member);

        StudyGroup group = getGroupOrThrow(groupId);
        notificationService.notifyStudent(group.getCreatorUsername(), "STUDY_GROUP_JOINED",
                nicknameOf(username) + "님이 \"" + group.getName() + "\" 그룹에 참여했어요!", null);
    }

    @Transactional
    public void declineInvite(Long groupId, String username) {
        StudyGroupMember member = requireMembership(groupId, username, "INVITED");
        member.decline();
        studyGroupMemberRepository.save(member);
    }

    @Transactional
    public void leaveGroup(Long groupId, String username) {
        StudyGroup group = getGroupOrThrow(groupId);
        if (group.getCreatorUsername().equals(username)) {
            throw new IllegalStateException("그룹장은 그룹을 나갈 수 없어요. 그룹을 삭제해주세요.");
        }
        StudyGroupMember member = requireMembership(groupId, username, "ACTIVE");
        member.leave();
        studyGroupMemberRepository.save(member);
    }

    @Transactional
    public void deleteGroup(Long groupId, String username) {
        StudyGroup group = getGroupOrThrow(groupId);
        if (!group.getCreatorUsername().equals(username)) {
            throw new IllegalStateException("그룹장만 그룹을 삭제할 수 있어요.");
        }
        for (StudyGroupGoal goal : studyGroupGoalRepository.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            studyGroupGoalAchievementRepository.deleteByGoalId(goal.getId());
        }
        studyGroupGoalRepository.deleteByGroupId(groupId);
        studyGroupNoteRepository.deleteByGroupId(groupId);
        studyGroupMemberRepository.deleteByGroupId(groupId);
        studyGroupRepository.deleteById(groupId);
    }

    // ---------- 그룹 노트 ----------

    @Transactional
    public void postNote(Long groupId, String username, PostStudyGroupNoteRequest request) {
        requireMembership(groupId, username, "ACTIVE");
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalStateException("노트 내용을 입력해주세요.");
        }
        String message = request.message().trim();
        if (message.length() > NOTE_MAX_LENGTH) {
            throw new IllegalStateException("노트는 " + NOTE_MAX_LENGTH + "자 이내로 적어주세요.");
        }

        studyGroupNoteRepository.save(StudyGroupNote.create(groupId, username, message));

        StudyGroup group = getGroupOrThrow(groupId);
        String myNickname = nicknameOf(username);
        String preview = message.length() > 30 ? message.substring(0, 30) + "..." : message;
        for (StudyGroupMember member : studyGroupMemberRepository.findByGroupId(groupId)) {
            if ("ACTIVE".equals(member.getStatus()) && !member.getUsername().equals(username)) {
                notificationService.notifyStudent(member.getUsername(), "STUDY_GROUP_NOTE",
                        "[" + group.getName() + "] " + myNickname + "님: " + preview, null);
            }
        }
    }

    @Transactional
    public List<StudyGroupNoteResponse> getNotes(Long groupId, String username) {
        requireMembership(groupId, username, null);
        return studyGroupNoteRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(n -> new StudyGroupNoteResponse(n.getId(), n.getSenderUsername(), nicknameOf(n.getSenderUsername()),
                        n.getMessage(), n.getCreatedAt().format(DATETIME_FORMAT)))
                .toList();
    }

    // ---------- 그룹 목표 ----------

    @Transactional
    public StudyGroupGoalResponse createGoal(Long groupId, String username, CreateStudyGroupGoalRequest request) {
        requireMembership(groupId, username, "ACTIVE");
        if (request.type() == null || !VALID_GOAL_TYPES.contains(request.type())) {
            throw new IllegalStateException("목표 종류를 다시 선택해주세요.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalStateException("목표 이름을 입력해주세요.");
        }
        if (request.targetValue() <= 0) {
            throw new IllegalStateException("목표 값은 1 이상이어야 해요.");
        }

        StudyGroupGoal goal = studyGroupGoalRepository.save(
                StudyGroupGoal.create(groupId, request.type(), request.title(), request.targetValue(), username));

        StudyGroup group = getGroupOrThrow(groupId);
        String myNickname = nicknameOf(username);
        for (StudyGroupMember member : studyGroupMemberRepository.findByGroupId(groupId)) {
            if ("ACTIVE".equals(member.getStatus()) && !member.getUsername().equals(username)) {
                notificationService.notifyStudent(member.getUsername(), "STUDY_GROUP_GOAL_CREATED",
                        "[" + group.getName() + "] " + myNickname + "님이 같이 목표를 만들었어요: \"" + request.title() + "\"", null);
            }
        }

        return toGoalResponse(goal);
    }

    @Transactional
    public List<StudyGroupGoalResponse> getGoals(Long groupId, String username) {
        requireMembership(groupId, username, null);
        return studyGroupGoalRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(this::toGoalResponse)
                .toList();
    }

    @Transactional
    public void deleteGoal(Long goalId, String username) {
        StudyGroupGoal goal = studyGroupGoalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalStateException("목표를 찾을 수 없어요."));
        StudyGroup group = getGroupOrThrow(goal.getGroupId());
        boolean canDelete = goal.getCreatedByUsername().equals(username) || group.getCreatorUsername().equals(username);
        if (!canDelete) {
            throw new IllegalStateException("이 목표를 삭제할 수 없어요.");
        }
        studyGroupGoalAchievementRepository.deleteByGoalId(goalId);
        studyGroupGoalRepository.deleteById(goalId);
    }

    private StudyGroupGoalResponse toGoalResponse(StudyGroupGoal goal) {
        List<StudyGroupMember> activeMembers = studyGroupMemberRepository.findByGroupId(goal.getGroupId()).stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .toList();

        List<StudyGroupGoalMemberProgressResponse> memberProgress = new ArrayList<>();
        int achievedCount = 0;

        for (StudyGroupMember member : activeMembers) {
            int progress = calculateProgress(goal, member.getUsername());
            boolean alreadyAchieved = studyGroupGoalAchievementRepository
                    .findByGoalIdAndUsername(goal.getId(), member.getUsername()).isPresent();

            if (progress >= goal.getTargetValue() && !alreadyAchieved) {
                studyGroupGoalAchievementRepository.save(StudyGroupGoalAchievement.create(goal.getId(), member.getUsername()));
                alreadyAchieved = true;
            }
            if (alreadyAchieved) achievedCount++;

            memberProgress.add(new StudyGroupGoalMemberProgressResponse(
                    member.getUsername(), nicknameOf(member.getUsername()), Math.min(progress, goal.getTargetValue()), alreadyAchieved));
        }

        boolean allAchieved = !activeMembers.isEmpty() && achievedCount == activeMembers.size();
        if (allAchieved && goal.getCompletedAt() == null) {
            goal.markCompleted();
            studyGroupGoalRepository.save(goal);
            StudyGroup group = getGroupOrThrow(goal.getGroupId());
            for (StudyGroupMember member : activeMembers) {
                notificationService.notifyStudent(member.getUsername(), "STUDY_GROUP_GOAL_ACHIEVED",
                        "[" + group.getName() + "] 다같이 \"" + goal.getTitle() + "\" 목표를 달성했어요! 축하해요 🎉", null);
            }
        }

        return new StudyGroupGoalResponse(goal.getId(), goal.getType(), goal.getTitle(), goal.getTargetValue(),
                memberProgress, goal.getCompletedAt() != null, goal.getCreatedAt().format(DATETIME_FORMAT));
    }

    private int calculateProgress(StudyGroupGoal goal, String username) {
        LocalDateTime since = goal.getCreatedAt();
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

    // ---------- 공통 ----------

    private StudyGroup getGroupOrThrow(Long groupId) {
        return studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalStateException("그룹을 찾을 수 없어요."));
    }

    // requiredStatus가 null이면 상태 상관없이(DECLINED 제외) 멤버이기만 하면 통과
    private StudyGroupMember requireMembership(Long groupId, String username, String requiredStatus) {
        StudyGroupMember member = studyGroupMemberRepository.findByGroupIdAndUsername(groupId, username)
                .orElseThrow(() -> new IllegalStateException("이 그룹의 멤버가 아니에요."));
        if (requiredStatus != null && !requiredStatus.equals(member.getStatus())) {
            throw new IllegalStateException("권한이 없어요.");
        }
        if (requiredStatus == null && "DECLINED".equals(member.getStatus())) {
            throw new IllegalStateException("이 그룹의 멤버가 아니에요.");
        }
        return member;
    }

    private StudyGroupResponse toResponse(StudyGroup group, String viewerUsername) {
        List<StudyGroupMemberResponse> members = studyGroupMemberRepository.findByGroupId(group.getId()).stream()
                .filter(m -> !"DECLINED".equals(m.getStatus()))
                .map(m -> new StudyGroupMemberResponse(m.getUsername(), nicknameOf(m.getUsername()), m.getStatus(),
                        m.getUsername().equals(group.getCreatorUsername())))
                .toList();

        String myStatus = studyGroupMemberRepository.findByGroupIdAndUsername(group.getId(), viewerUsername)
                .map(StudyGroupMember::getStatus)
                .orElse("NONE");

        return new StudyGroupResponse(group.getId(), group.getName(), group.getCreatorUsername(),
                nicknameOf(group.getCreatorUsername()), members, myStatus,
                group.getCreatorUsername().equals(viewerUsername), group.getCreatedAt().format(DATETIME_FORMAT));
    }

    private String nicknameOf(String username) {
        return userRepository.findByUsername(username).map(User::getNickname).orElse("(알 수 없음)");
    }
}