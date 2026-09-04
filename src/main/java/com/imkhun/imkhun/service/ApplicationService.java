package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.AdminApplicationResponse;
import com.imkhun.imkhun.dto.ApplicationResponse;
import com.imkhun.imkhun.dto.CreateApplicationRequest;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final Set<String> VALID_STUDY_TYPES = Set.of("TOGETHER", "VIDEO");
    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "APPROVED");

    public ApplicationService(ApplicationRepository applicationRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    public ApplicationResponse createApplication(String username, CreateApplicationRequest request) {
        if (request.studyType() == null || !VALID_STUDY_TYPES.contains(request.studyType())) {
            throw new IllegalStateException("학습 방식을 선택해주세요.");
        }
        if (request.courseName() == null || request.courseName().isBlank()) {
            throw new IllegalStateException("과목을 선택해주세요.");
        }
        if (request.contact() == null || request.contact().isBlank()) {
            throw new IllegalStateException("연락처를 입력해주세요.");
        }

        Application application = Application.create(
                username,
                request.studyType(),
                request.courseName(),
                request.contact(),
                request.memo()
        );
        Application saved = applicationRepository.save(application);
        return toResponse(saved);
    }

    public List<ApplicationResponse> getMyApplications(String username) {
        return applicationRepository.findByUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 관리자 - 전체 신청 목록 (누가 신청했는지 닉네임/이메일 포함)
    public List<AdminApplicationResponse> getAllApplicationsForAdmin() {
        return applicationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(app -> {
                    User user = userRepository.findByUsername(app.getUsername()).orElse(null);
                    String nickname = user != null ? user.getNickname() : app.getUsername();
                    String email = user != null ? user.getEmail() : null;
                    return new AdminApplicationResponse(
                            app.getId(), nickname, email, app.getStudyType(), app.getCourseName(),
                            app.getContact(), app.getMemo(), app.getStatus(),
                            app.getCreatedAt().format(DATE_FORMAT), app.getStudentNumber()
                    );
                })
                .toList();
    }

    // 관리자 - 신청 상태 변경 (승인대기 <-> 승인완료) — 승인되는 순간 학생번호를 자동 생성함
    public void updateStatus(Long applicationId, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalStateException("올바르지 않은 상태예요.");
        }
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        application.changeStatus(status);

        if ("APPROVED".equals(status) && application.getStudentNumber() == null) {
            application.assignStudentNumber(generateStudentNumber(application.getCourseName()));
        }

        applicationRepository.save(application);
    }

    // 예: "일본어 1급" -> "2026_Japanese_Level1_01"
    private String generateStudentNumber(String courseName) {
        String year = String.valueOf(LocalDate.now().getYear());
        String language = extractLanguage(courseName);
        String level = extractLevel(courseName);
        String prefix = year + "_" + language + "_" + level + "_";

        long count = applicationRepository.countByStudentNumberStartingWith(prefix);
        String sequence = String.format("%02d", count + 1);
        return prefix + sequence;
    }

    // 신청서 과목명을 자료 시스템에서 쓰는 언어 코드로 변환 (KWZM Center에서 자료 필터링할 때 씀)
    public String extractLanguageCode(String courseName) {
        if (courseName.startsWith("한국어")) return "korean";
        if (courseName.startsWith("일본어")) return "japanese";
        if (courseName.startsWith("태국어")) return "thai";
        if (courseName.startsWith("영어")) return "english";
        if (courseName.startsWith("컴퓨터")) return "computer";
        return "other";
    }

    private String extractLanguage(String courseName) {
        if (courseName.startsWith("한국어")) return "Korean";
        if (courseName.startsWith("일본어")) return "Japanese";
        if (courseName.startsWith("태국어")) return "Thai";
        if (courseName.startsWith("영어")) return "English";
        if (courseName.startsWith("컴퓨터")) return "Computer";
        return "Other";
    }

    private String extractLevel(String courseName) {
        if (courseName.contains("기초")) return "Basic";
        if (courseName.contains("1급")) return "Level1";
        if (courseName.contains("2급")) return "Level2";
        if (courseName.contains("3급")) return "Level3";
        if (courseName.contains("4급")) return "Level4";
        if (courseName.contains("페이지메이커")) return "PageMaker";
        if (courseName.contains("포토샵")) return "Photoshop";
        // 이미 영어인 컴퓨터 세부 과목(Word, Excel, PowerPoint 등)은 과목명 뒷부분을 그대로 씀
        String[] parts = courseName.trim().split("\\s+");
        return parts.length > 1 ? parts[1] : "General";
    }

    private ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getStudyType(),
                application.getCourseName(),
                application.getContact(),
                application.getMemo(),
                application.getStatus(),
                application.getCreatedAt().format(DATE_FORMAT),
                application.getStudentNumber()
        );
    }
}