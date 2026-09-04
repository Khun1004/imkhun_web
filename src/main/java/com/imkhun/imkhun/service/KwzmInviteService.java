package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.KwzmLanguageInvite;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.InvitedStudentResponse;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.KwzmLanguageInviteRepository;
import com.imkhun.imkhun.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class KwzmInviteService {

    private final KwzmLanguageInviteRepository inviteRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public KwzmInviteService(KwzmLanguageInviteRepository inviteRepository,
                             ApplicationRepository applicationRepository,
                             UserRepository userRepository) {
        this.inviteRepository = inviteRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<InvitedStudentResponse> getInvitedStudents(String language, String contentType) {
        return inviteRepository.findByLanguageAndContentTypeOrderByInvitedAtDesc(language, contentType)
                .stream()
                .map(invite -> {
                    Application app = applicationRepository.findByStudentNumber(invite.getStudentNumber()).orElse(null);
                    String nickname = "(알 수 없음)";
                    String courseName = "";
                    if (app != null) {
                        courseName = app.getCourseName();
                        nickname = userRepository.findByUsername(app.getUsername())
                                .map(User::getNickname)
                                .orElse("(알 수 없음)");
                    }
                    return new InvitedStudentResponse(
                            invite.getStudentNumber(), nickname, courseName,
                            invite.getInvitedAt().format(DATE_FORMAT)
                    );
                })
                .toList();
    }

    @Transactional
    public void inviteStudent(String language, String contentType, String studentNumber) {
        if (studentNumber == null || studentNumber.isBlank()) {
            throw new IllegalStateException("학생번호를 입력해주세요.");
        }
        Application app = applicationRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new IllegalStateException("이 학생번호를 찾을 수 없어요. 다시 확인해주세요."));
        if (!"APPROVED".equals(app.getStatus())) {
            throw new IllegalStateException("승인된 학생만 초대할 수 있어요.");
        }
        if (inviteRepository.existsByLanguageAndContentTypeAndStudentNumber(language, contentType, studentNumber)) {
            throw new IllegalStateException("이미 초대된 학생이에요.");
        }
        inviteRepository.save(KwzmLanguageInvite.create(language, contentType, studentNumber));
    }

    @Transactional
    public void removeStudent(String language, String contentType, String studentNumber) {
        inviteRepository.findByLanguageAndContentTypeAndStudentNumber(language, contentType, studentNumber)
                .ifPresent(inviteRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean isInvited(String language, String contentType, String studentNumber) {
        return inviteRepository.existsByLanguageAndContentTypeAndStudentNumber(language, contentType, studentNumber);
    }
}