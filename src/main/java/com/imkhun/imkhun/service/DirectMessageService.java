package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.DirectMessage;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.AdminMessageThreadResponse;
import com.imkhun.imkhun.dto.DirectMessageResponse;
import com.imkhun.imkhun.repository.DirectMessageRepository;
import com.imkhun.imkhun.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

// 선생님(관리자)과 학생 사이의 1:1 메시지
@Service
public class DirectMessageService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public DirectMessageService(DirectMessageRepository directMessageRepository, UserRepository userRepository,
                                NotificationService notificationService) {
        this.directMessageRepository = directMessageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void sendFromStudent(String username, String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) throw new IllegalStateException("메시지 내용을 입력해주세요.");
        if (trimmed.length() > 2000) throw new IllegalStateException("메시지가 너무 길어요. 2000자 이내로 입력해주세요.");

        directMessageRepository.save(DirectMessage.fromStudent(username, trimmed));
        notificationService.notifyAdmin("STUDENT_MESSAGE", username + " 학생이 메시지를 보냈어요.", null);
    }

    @Transactional
    public void sendFromAdmin(String username, String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) throw new IllegalStateException("메시지 내용을 입력해주세요.");
        if (trimmed.length() > 2000) throw new IllegalStateException("메시지가 너무 길어요. 2000자 이내로 입력해주세요.");

        directMessageRepository.save(DirectMessage.fromAdmin(username, trimmed));
        notificationService.notifyStudent(username, "TEACHER_MESSAGE", "선생님이 메시지를 보냈어요.", null);
    }

    // 학생이 본인 대화창을 열 때 — 관리자가 보낸 메시지를 전부 "읽음" 처리하고 돌려줌
    @Transactional
    public List<DirectMessageResponse> getThreadForStudent(String username) {
        List<DirectMessage> messages = directMessageRepository.findByUsernameOrderByCreatedAtAsc(username);
        for (DirectMessage m : messages) {
            if ("ADMIN".equals(m.getSenderType()) && !m.isReadByStudent()) {
                m.markReadByStudent();
                directMessageRepository.save(m);
            }
        }
        return messages.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForStudent(String username) {
        return directMessageRepository.countByUsernameAndSenderTypeAndReadByStudentFalse(username, "ADMIN");
    }

    // 관리자가 특정 학생 대화창을 열 때 — 그 학생이 보낸 메시지를 전부 "읽음" 처리하고 돌려줌
    @Transactional
    public List<DirectMessageResponse> getThreadForAdmin(String username) {
        List<DirectMessage> messages = directMessageRepository.findByUsernameOrderByCreatedAtAsc(username);
        for (DirectMessage m : messages) {
            if ("STUDENT".equals(m.getSenderType()) && !m.isReadByAdmin()) {
                m.markReadByAdmin();
                directMessageRepository.save(m);
            }
        }
        return messages.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForAdmin() {
        return directMessageRepository.countBySenderTypeAndReadByAdminFalse("STUDENT");
    }

    // 관리자 "학생 메시지" 화면 왼쪽의 대화방 목록 — 최근 메시지가 온 순서로 정렬
    @Transactional(readOnly = true)
    public List<AdminMessageThreadResponse> getThreadsForAdmin() {
        List<String> usernames = directMessageRepository.findDistinctUsernames();
        return usernames.stream()
                .map(username -> {
                    List<DirectMessage> messages = directMessageRepository.findByUsernameOrderByCreatedAtAsc(username);
                    if (messages.isEmpty()) return null;
                    DirectMessage last = messages.get(messages.size() - 1);
                    long unread = directMessageRepository.countByUsernameAndSenderTypeAndReadByAdminFalse(username, "STUDENT");
                    User user = userRepository.findByUsername(username).orElse(null);
                    String nickname = user != null ? user.getNickname() : username;
                    return new AdminMessageThreadResponse(username, nickname, last.getContent(),
                            last.getCreatedAt().format(DATE_FORMAT), unread);
                })
                .filter(t -> t != null)
                .sorted(Comparator.comparing(AdminMessageThreadResponse::lastMessageAt).reversed())
                .toList();
    }

    private DirectMessageResponse toResponse(DirectMessage m) {
        return new DirectMessageResponse(m.getId(), m.getSenderType(), m.getContent(), m.getCreatedAt().format(DATE_FORMAT));
    }
}