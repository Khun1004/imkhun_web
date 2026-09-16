package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.Assignment;
import com.imkhun.imkhun.dto.AssignmentResponse;
import com.imkhun.imkhun.dto.CreateAssignmentRequest;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.AssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AssignmentRepository assignmentRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public AssignmentService(AssignmentRepository assignmentRepository, ApplicationRepository applicationRepository,
                             NotificationService notificationService) {
        this.assignmentRepository = assignmentRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public AssignmentResponse createAssignment(Long applicationId, CreateAssignmentRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalStateException("숙제 제목을 입력해주세요.");
        }
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));

        LocalDate dueDate = null;
        if (request.dueDate() != null && !request.dueDate().isBlank()) {
            try {
                dueDate = LocalDate.parse(request.dueDate());
            } catch (Exception e) {
                throw new IllegalStateException("제출 기한 형식이 올바르지 않아요.");
            }
        }

        Assignment saved = assignmentRepository.save(
                Assignment.create(applicationId, request.title(), request.description(), dueDate));

        notificationService.notifyStudent(application.getUsername(), "NEW_ASSIGNMENT",
                application.getCourseName() + " 강의에 새 숙제가 등록됐어요: " + request.title(), null);

        return toResponse(saved, application.getCourseName());
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getForApplication(Long applicationId) {
        String courseName = applicationRepository.findById(applicationId)
                .map(Application::getCourseName).orElse("");
        return assignmentRepository.findByApplicationIdOrderByDueDateAscCreatedAtDesc(applicationId)
                .stream()
                .map(a -> toResponse(a, courseName))
                .toList();
    }

    // 학생 마이페이지 "내 숙제" — 본인의 모든 신청(Application)에 걸친 숙제를 한번에 모아서 보여줌
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getForStudent(String username) {
        List<Application> myApplications = applicationRepository.findByUsernameOrderByCreatedAtDesc(username);
        List<Long> applicationIds = myApplications.stream().map(Application::getId).toList();
        Map<Long, String> courseNameById = myApplications.stream()
                .collect(Collectors.toMap(Application::getId, Application::getCourseName));

        return assignmentRepository.findByApplicationIdInOrderByCompletedAscDueDateAsc(applicationIds)
                .stream()
                .map(a -> toResponse(a, courseNameById.getOrDefault(a.getApplicationId(), "")))
                .toList();
    }

    @Transactional
    public void toggleComplete(Long assignmentId, String username, boolean completed) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("숙제를 찾을 수 없어요."));
        Application application = applicationRepository.findById(assignment.getApplicationId())
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        if (!application.getUsername().equals(username)) {
            throw new IllegalStateException("본인의 숙제만 체크할 수 있어요.");
        }

        if (completed) {
            assignment.markComplete();
            notificationService.notifyAdmin("ASSIGNMENT_COMPLETED",
                    username + "님이 \"" + assignment.getTitle() + "\" 숙제를 완료했어요.", null);
        } else {
            assignment.markIncomplete();
        }
        assignmentRepository.save(assignment);
    }

    public void deleteAssignment(Long id) {
        assignmentRepository.deleteById(id);
    }

    private AssignmentResponse toResponse(Assignment assignment, String courseName) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getApplicationId(),
                courseName,
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate() != null ? assignment.getDueDate().toString() : null,
                assignment.isCompleted(),
                assignment.getCreatedAt().format(DATETIME_FORMAT)
        );
    }
}