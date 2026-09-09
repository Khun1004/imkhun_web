package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.AdminSentFile;
import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.dto.SendFileRequest;
import com.imkhun.imkhun.dto.SentFileResponse;
import com.imkhun.imkhun.repository.AdminSentFileRepository;
import com.imkhun.imkhun.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminFileService {

    private final AdminSentFileRepository adminSentFileRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final Set<String> VALID_CATEGORIES = Set.of("CERTIFICATE", "EXAM");
    private static final Map<String, String> CATEGORY_LABEL = Map.of("CERTIFICATE", "자격증", "EXAM", "시험 자료");

    public AdminFileService(AdminSentFileRepository adminSentFileRepository, ApplicationRepository applicationRepository,
                            NotificationService notificationService) {
        this.adminSentFileRepository = adminSentFileRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    // 관리자 - 특정 학생의 강의(신청)에 파일 보내기
    @Transactional
    public SentFileResponse sendFile(Long applicationId, SendFileRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));

        if (request.category() == null || !VALID_CATEGORIES.contains(request.category())) {
            throw new IllegalStateException("올바른 종류(자격증/시험 자료)를 선택해주세요.");
        }
        if (request.fileName() == null || request.fileName().isBlank()) {
            throw new IllegalStateException("파일을 선택해주세요.");
        }
        if (request.fileData() == null || request.fileData().isBlank()) {
            throw new IllegalStateException("파일을 선택해주세요.");
        }

        AdminSentFile saved = adminSentFileRepository.save(
                AdminSentFile.create(applicationId, request.category(), request.fileName(), request.fileData()));

        notificationService.notifyStudent(application.getUsername(), "ADMIN_FILE_SENT",
                application.getCourseName() + " " + CATEGORY_LABEL.get(request.category()) + "이(가) 도착했어요. 마이페이지에서 확인해주세요.", null);

        return toResponse(saved, application.getCourseName());
    }

    // 관리자 - 특정 신청(강의)에 보낸 파일 목록
    @Transactional(readOnly = true)
    public List<SentFileResponse> getFilesForApplication(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        return adminSentFileRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId)
                .stream()
                .map(f -> toResponse(f, application.getCourseName()))
                .toList();
    }

    @Transactional
    public void deleteFile(Long fileId) {
        if (!adminSentFileRepository.existsById(fileId)) {
            throw new IllegalStateException("파일을 찾을 수 없어요.");
        }
        adminSentFileRepository.deleteById(fileId);
    }

    // 학생 - 본인의 모든 강의에 대해 받은 파일 전체 조회
    @Transactional(readOnly = true)
    public List<SentFileResponse> getFilesForStudent(String username) {
        List<Application> myApplications = applicationRepository.findByUsernameOrderByCreatedAtDesc(username);
        Map<Long, String> courseNameByApplicationId = myApplications.stream()
                .collect(Collectors.toMap(Application::getId, Application::getCourseName));

        List<Long> applicationIds = myApplications.stream().map(Application::getId).toList();
        if (applicationIds.isEmpty()) return List.of();

        return adminSentFileRepository.findByApplicationIdInOrderByCreatedAtDesc(applicationIds)
                .stream()
                .map(f -> toResponse(f, courseNameByApplicationId.get(f.getApplicationId())))
                .toList();
    }

    private SentFileResponse toResponse(AdminSentFile file, String courseName) {
        return new SentFileResponse(file.getId(), file.getCategory(), file.getFileName(), file.getFileData(),
                courseName, file.getCreatedAt().format(DATE_FORMAT));
    }
}