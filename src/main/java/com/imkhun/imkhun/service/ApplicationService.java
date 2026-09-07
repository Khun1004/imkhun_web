package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.AdminApplicationResponse;
import com.imkhun.imkhun.dto.ApplicationResponse;
import com.imkhun.imkhun.dto.ChangeCourseRequest;
import com.imkhun.imkhun.dto.CreateApplicationRequest;
import com.imkhun.imkhun.dto.UpdateApplicationPaymentRequest;
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
    private final NotificationService notificationService;
    private final KwzmInviteService kwzmInviteService;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final Set<String> VALID_STUDY_TYPES = Set.of("TOGETHER", "VIDEO");
    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "APPROVED");

    public ApplicationService(ApplicationRepository applicationRepository, UserRepository userRepository,
                              NotificationService notificationService, KwzmInviteService kwzmInviteService) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.kwzmInviteService = kwzmInviteService;
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

        String nickname = userRepository.findByUsername(username).map(User::getNickname).orElse(username);
        notificationService.notifyAdmin("NEW_APPLICATION",
                nickname + "님이 " + request.courseName() + " 강의를 신청했어요.", null);

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
                            app.getCreatedAt().format(DATE_FORMAT), app.getStudentNumber(),
                            app.hasPaymentInfo(), app.getPaymentMethod(), app.getAmount(),
                            app.getAmountReason(), app.getMaterialGuide(), app.getClassGuide(),
                            app.getPaymentConfirmedByStudentAt() != null, app.getPaymentConfirmedByAdminAt() != null,
                            app.getPaymentConfirmedByStudentAt() != null ? app.getPaymentConfirmedByStudentAt().format(DATE_FORMAT) : null,
                            app.getReceiptImage()
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
        boolean wasAlreadyApproved = "APPROVED".equals(application.getStatus());
        application.changeStatus(status);

        if ("APPROVED".equals(status) && application.getStudentNumber() == null) {
            application.assignStudentNumber(generateStudentNumber(application.getCourseName()));
        }

        applicationRepository.save(application);

        if ("APPROVED".equals(status) && !wasAlreadyApproved) {
            notificationService.notifyStudent(application.getUsername(), "APPLICATION_APPROVED",
                    "신청하신 " + application.getCourseName() + " 강의가 승인됐어요!", null);
        }
    }

    // 관리자 - 강의(과목/학습방식) 변경. 학생번호가 이미 있었다면 새로 발급하고, 기존 KWZM 초대들을 새 번호로 옮겨줌
    public void changeCourse(Long applicationId, ChangeCourseRequest request) {
        if (request.studyType() == null || !VALID_STUDY_TYPES.contains(request.studyType())) {
            throw new IllegalStateException("학습 방식을 선택해주세요.");
        }
        if (request.courseName() == null || request.courseName().isBlank()) {
            throw new IllegalStateException("과목을 선택해주세요.");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));

        String oldStudentNumber = application.getStudentNumber();
        boolean courseActuallyChanged = !request.courseName().equals(application.getCourseName());
        application.changeCourse(request.studyType(), request.courseName());

        if (oldStudentNumber != null && courseActuallyChanged) {
            String newStudentNumber = generateStudentNumber(request.courseName());
            application.assignStudentNumber(newStudentNumber);
            kwzmInviteService.migrateStudentNumber(oldStudentNumber, newStudentNumber);
            applicationRepository.save(application);

            notificationService.notifyStudent(application.getUsername(), "COURSE_CHANGED",
                    "신청하신 강의가 " + request.courseName() + "(으)로 변경됐어요. 학생번호도 " + newStudentNumber + "(으)로 새로 배정됐어요.", null);
        } else {
            applicationRepository.save(application);
            if (courseActuallyChanged) {
                notificationService.notifyStudent(application.getUsername(), "COURSE_CHANGED",
                        "신청하신 강의가 " + request.courseName() + "(으)로 변경됐어요.", null);
            }
        }
    }

    // 관리자 - 결제 안내 등록/수정 (승인된 학생에게만)
    public void updatePaymentInfo(Long applicationId, UpdateApplicationPaymentRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        if (!"APPROVED".equals(application.getStatus())) {
            throw new IllegalStateException("승인된 학생에게만 결제 안내를 등록할 수 있어요.");
        }

        boolean hadPaymentInfoBefore = application.hasPaymentInfo();
        application.updatePaymentInfo(request.paymentMethod(), request.amount(), request.amountReason(),
                request.materialGuide(), request.classGuide());
        applicationRepository.save(application);

        if (!hadPaymentInfoBefore && application.hasPaymentInfo()) {
            notificationService.notifyStudent(application.getUsername(), "PAYMENT_INFO_REGISTERED",
                    "결제 안내가 등록됐어요. 마이페이지에서 확인해주세요.", null);
        }
    }

    // 학생 - "입금했어요" 버튼. 본인 신청 내역에만 누를 수 있어요. 영수증 이미지는 선택이에요.
    public void confirmPaymentByStudent(Long applicationId, String username, String receiptImage) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        if (!application.getUsername().equals(username)) {
            throw new IllegalStateException("본인 신청 내역만 확인할 수 있어요.");
        }
        if (!application.hasPaymentInfo()) {
            throw new IllegalStateException("아직 등록된 결제 안내가 없어요.");
        }
        if (application.getPaymentConfirmedByStudentAt() != null) {
            return;
        }

        application.confirmPaymentByStudent(receiptImage);
        applicationRepository.save(application);

        String nickname = userRepository.findByUsername(username).map(User::getNickname).orElse(username);
        String receiptNote = receiptImage != null && !receiptImage.isBlank() ? " (영수증 첨부됨)" : "";
        notificationService.notifyAdmin("PAYMENT_CONFIRMED_BY_STUDENT",
                nickname + "님이 " + application.getCourseName() + " 결제를 완료했다고 알려왔어요." + receiptNote, null);
    }

    // 관리자 - 입금 확인. 학생에게 확인됐다는 알림을 보내줌
    public void confirmPaymentByAdmin(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        if (application.getPaymentConfirmedByAdminAt() != null) {
            return;
        }

        application.confirmPaymentByAdmin();
        applicationRepository.save(application);

        notificationService.notifyStudent(application.getUsername(), "PAYMENT_CONFIRMED_BY_ADMIN",
                application.getCourseName() + " 입금이 확인됐어요. 감사합니다!", null);
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
                application.getStudentNumber(),
                application.hasPaymentInfo(),
                application.getPaymentMethod(),
                application.getAmount(),
                application.getAmountReason(),
                application.getMaterialGuide(),
                application.getClassGuide(),
                application.getPaymentConfirmedByStudentAt() != null,
                application.getPaymentConfirmedByAdminAt() != null
        );
    }
}