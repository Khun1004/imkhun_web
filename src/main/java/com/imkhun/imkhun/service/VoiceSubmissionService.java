package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.VoiceSubmission;
import com.imkhun.imkhun.dto.CreateVoiceSubmissionRequest;
import com.imkhun.imkhun.dto.VoiceSubmissionResponse;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.VoiceSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class VoiceSubmissionService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final VoiceSubmissionRepository voiceSubmissionRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public VoiceSubmissionService(VoiceSubmissionRepository voiceSubmissionRepository, ApplicationRepository applicationRepository,
                                  NotificationService notificationService) {
        this.voiceSubmissionRepository = voiceSubmissionRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void submit(String username, CreateVoiceSubmissionRequest request) {
        Application application = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        if (!application.getUsername().equals(username)) {
            throw new IllegalStateException("본인의 강의에만 제출할 수 있어요.");
        }
        if (request.audioData() == null || request.audioData().isBlank()) {
            throw new IllegalStateException("녹음된 파일이 없어요.");
        }

        voiceSubmissionRepository.save(VoiceSubmission.create(
                request.applicationId(), username, application.getCourseName(), request.title(), request.audioData()));

        notificationService.notifyAdmin("NEW_VOICE_SUBMISSION",
                application.getCourseName() + " 강의에 새 발음 녹음이 도착했어요.", null);
    }

    @Transactional(readOnly = true)
    public List<VoiceSubmissionResponse> getForStudent(String username) {
        return voiceSubmissionRepository.findByUsernameOrderByCreatedAtDesc(username).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VoiceSubmissionResponse> getAllForAdmin() {
        return voiceSubmissionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void addComment(Long id, String comment) {
        VoiceSubmission submission = voiceSubmissionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("제출 내역을 찾을 수 없어요."));
        submission.addComment(comment);
        voiceSubmissionRepository.save(submission);

        notificationService.notifyStudent(submission.getUsername(), "VOICE_COMMENT_ADDED",
                submission.getCourseName() + " 발음 녹음에 선생님이 코멘트를 남겼어요.", null);
    }

    @Transactional
    public void delete(Long id) {
        voiceSubmissionRepository.deleteById(id);
    }

    private VoiceSubmissionResponse toResponse(VoiceSubmission submission) {
        return new VoiceSubmissionResponse(
                submission.getId(), submission.getUsername(), submission.getCourseName(), submission.getTitle(),
                submission.getAudioData(), submission.getAdminComment(), submission.getCreatedAt().format(DATETIME_FORMAT)
        );
    }
}