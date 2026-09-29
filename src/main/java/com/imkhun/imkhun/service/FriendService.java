package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.FriendNote;
import com.imkhun.imkhun.domain.Friendship;
import com.imkhun.imkhun.domain.StudentFriendCode;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.FriendBadgesResponse;
import com.imkhun.imkhun.dto.FriendCodeResponse;
import com.imkhun.imkhun.dto.FriendNoteResponse;
import com.imkhun.imkhun.dto.FriendResponse;
import com.imkhun.imkhun.dto.FriendScheduleResponse;
import com.imkhun.imkhun.repository.FriendNoteRepository;
import com.imkhun.imkhun.repository.FriendshipRepository;
import com.imkhun.imkhun.repository.StudentFriendCodeRepository;
import com.imkhun.imkhun.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

// 학생끼리 초대 코드로 서로 "친구"를 맺는 기능. 친구가 되면 시간표/노트/배지 같은 것을
// 서로 볼 수 있게 되는 다른 기능들의 기반이 됨
@Service
public class FriendService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    // 헷갈리는 글자(0/O, 1/I) 빼고 대문자+숫자로 6자리 코드를 만듦
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int NOTE_MAX_LENGTH = 300;

    private final StudentFriendCodeRepository studentFriendCodeRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AttendanceService attendanceService;
    private final BadgeService badgeService;
    private final FriendNoteRepository friendNoteRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public FriendService(StudentFriendCodeRepository studentFriendCodeRepository, FriendshipRepository friendshipRepository,
                         UserRepository userRepository, NotificationService notificationService,
                         AttendanceService attendanceService, BadgeService badgeService,
                         FriendNoteRepository friendNoteRepository) {
        this.studentFriendCodeRepository = studentFriendCodeRepository;
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.attendanceService = attendanceService;
        this.badgeService = badgeService;
        this.friendNoteRepository = friendNoteRepository;
    }

    @Transactional
    public FriendCodeResponse getOrCreateMyCode(String username) {
        StudentFriendCode existing = studentFriendCodeRepository.findByUsername(username).orElse(null);
        if (existing != null) {
            return new FriendCodeResponse(existing.getCode());
        }

        String code = generateUniqueCode();
        StudentFriendCode saved = studentFriendCodeRepository.save(StudentFriendCode.create(username, code));
        return new FriendCodeResponse(saved.getCode());
    }

    @Transactional
    public FriendResponse addFriendByCode(String username, String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalStateException("친구 코드를 입력해주세요.");
        }
        String code = rawCode.trim().toUpperCase();

        StudentFriendCode friendCode = studentFriendCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("코드를 찾을 수 없어요. 다시 확인해주세요."));

        String friendUsername = friendCode.getUsername();
        if (friendUsername.equals(username)) {
            throw new IllegalStateException("본인의 코드는 사용할 수 없어요.");
        }

        boolean alreadyFriends = findFriendship(username, friendUsername).isPresent();
        if (alreadyFriends) {
            throw new IllegalStateException("이미 친구예요.");
        }

        friendshipRepository.save(Friendship.create(username, friendUsername));

        String myNickname = userRepository.findByUsername(username).map(User::getNickname).orElse("친구");
        String friendNickname = userRepository.findByUsername(friendUsername).map(User::getNickname).orElse("친구");

        notificationService.notifyStudent(friendUsername, "FRIEND_ADDED",
                myNickname + "님과 친구가 되었어요!", null);
        notificationService.notifyStudent(username, "FRIEND_ADDED",
                friendNickname + "님과 친구가 되었어요!", null);

        return new FriendResponse(friendUsername, friendNickname, java.time.LocalDateTime.now().format(DATE_FORMAT));
    }

    @Transactional
    public List<FriendResponse> getMyFriends(String username) {
        return friendshipRepository.findByUsernameAOrUsernameB(username, username).stream()
                .map(f -> {
                    String friendUsername = f.getUsernameA().equals(username) ? f.getUsernameB() : f.getUsernameA();
                    String friendNickname = userRepository.findByUsername(friendUsername).map(User::getNickname).orElse("(알 수 없음)");
                    return new FriendResponse(friendUsername, friendNickname, f.getCreatedAt().format(DATE_FORMAT));
                })
                .toList();
    }

    @Transactional
    public FriendScheduleResponse getFriendSchedule(String username, String friendUsername) {
        if (findFriendship(username, friendUsername).isEmpty()) {
            throw new IllegalStateException("친구 사이일 때만 시간표를 볼 수 있어요.");
        }
        String friendNickname = userRepository.findByUsername(friendUsername).map(User::getNickname).orElse("(알 수 없음)");
        return new FriendScheduleResponse(friendUsername, friendNickname, attendanceService.getWeeklyScheduleForStudent(friendUsername));
    }

    @Transactional
    public FriendBadgesResponse getFriendBadges(String username, String friendUsername) {
        if (findFriendship(username, friendUsername).isEmpty()) {
            throw new IllegalStateException("친구 사이일 때만 배지를 볼 수 있어요.");
        }
        String friendNickname = userRepository.findByUsername(friendUsername).map(User::getNickname).orElse("(알 수 없음)");
        return new FriendBadgesResponse(friendUsername, friendNickname, badgeService.getBadgesForStudent(friendUsername));
    }

    @Transactional
    public void sendNote(String username, String friendUsername, String rawMessage) {
        if (findFriendship(username, friendUsername).isEmpty()) {
            throw new IllegalStateException("친구 사이일 때만 쪽지를 보낼 수 있어요.");
        }
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new IllegalStateException("쪽지 내용을 입력해주세요.");
        }
        String message = rawMessage.trim();
        if (message.length() > NOTE_MAX_LENGTH) {
            throw new IllegalStateException("쪽지는 " + NOTE_MAX_LENGTH + "자 이내로 적어주세요.");
        }

        friendNoteRepository.save(FriendNote.create(username, friendUsername, message));

        String myNickname = userRepository.findByUsername(username).map(User::getNickname).orElse("친구");
        String preview = message.length() > 30 ? message.substring(0, 30) + "..." : message;
        notificationService.notifyStudent(friendUsername, "FRIEND_NOTE", myNickname + "님이 쪽지를 보냈어요: " + preview, null);
    }

    @Transactional(readOnly = true)
    public List<FriendNoteResponse> getReceivedNotes(String username) {
        return friendNoteRepository.findByReceiverUsernameOrderByCreatedAtDesc(username).stream()
                .map(n -> {
                    String senderNickname = userRepository.findByUsername(n.getSenderUsername()).map(User::getNickname).orElse("(알 수 없음)");
                    return new FriendNoteResponse(n.getId(), n.getSenderUsername(), senderNickname, n.getMessage(), n.getCreatedAt().format(DATETIME_FORMAT));
                })
                .toList();
    }

    @Transactional
    public void removeFriend(String username, String friendUsername) {
        Friendship friendship = findFriendship(username, friendUsername)
                .orElseThrow(() -> new IllegalStateException("친구 관계를 찾을 수 없어요."));
        friendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public boolean isFriend(String usernameOne, String usernameTwo) {
        return findFriendship(usernameOne, usernameTwo).isPresent();
    }

    private Optional<Friendship> findFriendship(String usernameOne, String usernameTwo) {
        String usernameA = usernameOne.compareTo(usernameTwo) <= 0 ? usernameOne : usernameTwo;
        String usernameB = usernameOne.compareTo(usernameTwo) <= 0 ? usernameTwo : usernameOne;
        return friendshipRepository.findByUsernameAAndUsernameB(usernameA, usernameB);
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(secureRandom.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (studentFriendCodeRepository.existsByCode(code));
        return code;
    }
}