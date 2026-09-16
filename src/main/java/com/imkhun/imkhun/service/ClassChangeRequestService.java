package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.ClassChangeRequest;
import com.imkhun.imkhun.dto.ClassChangeRequestResponse;
import com.imkhun.imkhun.dto.CreateClassChangeRequest;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.ClassChangeRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClassChangeRequestService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ClassChangeRequestRepository classChangeRequestRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public ClassChangeRequestService(ClassChangeRequestRepository classChangeRequestRepository,
                                     ApplicationRepository applicationRepository,
                                     NotificationService notificationService) {
        this.classChangeRequestRepository = classChangeRequestRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    // 학생이 신청 — 본인 신청(Application)이 맞는지 확인하고 만듦
    @Transactional
    public ClassChangeRequestResponse createRequest(Long applicationId, String username, CreateClassChangeRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        if (!application.getUsername().equals(username)) {
            throw new IllegalStateException("본인의 강의에만 요청할 수 있어요.");
        }
        if ("TRIAL".equals(application.getStudyType())) {
            throw new IllegalStateException("무료체험은 취소/변경 요청 대상이 아니에요.");
        }
        if (!"CANCEL".equals(request.requestType()) && !"RESCHEDULE".equals(request.requestType())) {
            throw new IllegalStateException("요청 종류가 올바르지 않아요.");
        }
        if (request.classDate() == null || request.classDate().isBlank()) {
            throw new IllegalStateException("날짜를 선택해주세요.");
        }

        LocalDate classDate = parseDate(request.classDate(), "날짜");
        LocalDate requestedDate = null;
        if ("RESCHEDULE".equals(request.requestType())) {
            if (request.requestedDate() == null || request.requestedDate().isBlank()) {
                throw new IllegalStateException("희망하는 새 날짜를 선택해주세요.");
            }
            requestedDate = parseDate(request.requestedDate(), "희망 날짜");
        }

        ClassChangeRequest saved = classChangeRequestRepository.save(
                ClassChangeRequest.create(applicationId, request.requestType(), classDate, requestedDate, request.reason()));

        String typeLabel = "CANCEL".equals(request.requestType()) ? "취소" : "변경";
        notificationService.notifyAdmin("CLASS_CHANGE_REQUEST",
                application.getUsername() + "님이 " + application.getCourseName() + " " + classDate + " 수업 "
                        + typeLabel + "을 요청했어요.", null);

        return toResponse(saved, application);
    }

    @Transactional(readOnly = true)
    public List<ClassChangeRequestResponse> getForApplication(Long applicationId) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        return classChangeRequestRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId)
                .stream()
                .map(r -> toResponse(r, application))
                .toList();
    }

    // 학생 마이페이지 — 본인의 모든 신청에 걸친 요청을 한번에 모아서 보여줌
    @Transactional(readOnly = true)
    public List<ClassChangeRequestResponse> getForStudent(String username) {
        List<Application> myApplications = applicationRepository.findByUsernameOrderByCreatedAtDesc(username);
        List<Long> applicationIds = myApplications.stream().map(Application::getId).toList();
        Map<Long, Application> applicationById = myApplications.stream()
                .collect(Collectors.toMap(Application::getId, a -> a));

        return classChangeRequestRepository.findByApplicationIdInOrderByCreatedAtDesc(applicationIds)
                .stream()
                .map(r -> toResponse(r, applicationById.get(r.getApplicationId())))
                .toList();
    }

    // 관리자 — 아직 처리 안 한 요청 전체
    @Transactional(readOnly = true)
    public List<ClassChangeRequestResponse> getPendingForAdmin() {
        return classChangeRequestRepository.findByStatusOrderByCreatedAtAsc("PENDING")
                .stream()
                .map(r -> toResponse(r, applicationRepository.findById(r.getApplicationId()).orElse(null)))
                .toList();
    }

    @Transactional
    public void respond(Long id, boolean approved, String adminReply) {
        ClassChangeRequest request = classChangeRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("요청을 찾을 수 없어요."));
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));

        request.respond(approved ? "APPROVED" : "REJECTED", adminReply);
        classChangeRequestRepository.save(request);

        String typeLabel = "CANCEL".equals(request.getRequestType()) ? "취소" : "변경";
        String resultLabel = approved ? "승인" : "거절";
        notificationService.notifyStudent(application.getUsername(), "CLASS_CHANGE_RESPONDED",
                application.getCourseName() + " " + request.getClassDate() + " 수업 " + typeLabel + " 요청이 "
                        + resultLabel + "됐어요." + (adminReply != null && !adminReply.isBlank() ? " (" + adminReply + ")" : ""),
                null);
    }

    private LocalDate parseDate(String value, String fieldLabel) {
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new IllegalStateException(fieldLabel + " 형식이 올바르지 않아요.");
        }
    }

    private ClassChangeRequestResponse toResponse(ClassChangeRequest request, Application application) {
        return new ClassChangeRequestResponse(
                request.getId(),
                request.getApplicationId(),
                application != null ? application.getCourseName() : "-",
                application != null ? application.getUsername() : "-",
                request.getRequestType(),
                request.getClassDate().toString(),
                request.getRequestedDate() != null ? request.getRequestedDate().toString() : null,
                request.getReason(),
                request.getStatus(),
                request.getAdminReply(),
                request.getCreatedAt().format(DATETIME_FORMAT)
        );
    }
}