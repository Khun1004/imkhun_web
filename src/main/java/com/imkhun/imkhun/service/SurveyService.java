package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.SurveyResponse;
import com.imkhun.imkhun.dto.CreateSurveyRequest;
import com.imkhun.imkhun.dto.SurveyResponseDetail;
import com.imkhun.imkhun.dto.SurveySummaryResponse;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.SurveyResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SurveyService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SurveyResponseRepository surveyResponseRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public SurveyService(SurveyResponseRepository surveyResponseRepository, ApplicationRepository applicationRepository,
                         NotificationService notificationService) {
        this.surveyResponseRepository = surveyResponseRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void submitSurvey(Long applicationId, String username, CreateSurveyRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        if (!application.getUsername().equals(username)) {
            throw new IllegalStateException("본인의 강의에만 설문을 남길 수 있어요.");
        }
        if (surveyResponseRepository.existsByApplicationId(applicationId)) {
            throw new IllegalStateException("이미 설문을 제출하셨어요. 소중한 의견 감사해요!");
        }
        validateRating(request.contentRating());
        validateRating(request.teacherRating());
        validateRating(request.materialRating());
        validateRating(request.overallRating());

        surveyResponseRepository.save(SurveyResponse.create(
                applicationId, username, application.getCourseName(),
                request.contentRating(), request.teacherRating(), request.materialRating(),
                request.overallRating(), request.comment(), request.isAnonymous()
        ));

        notificationService.notifyAdmin("NEW_SURVEY", application.getCourseName() + " 강의에 새 만족도 설문이 도착했어요.", null);
    }

    @Transactional(readOnly = true)
    public boolean hasSubmitted(Long applicationId) {
        return surveyResponseRepository.existsByApplicationId(applicationId);
    }

    @Transactional(readOnly = true)
    public SurveySummaryResponse getSummaryForAdmin() {
        List<SurveyResponse> all = surveyResponseRepository.findAll();
        if (all.isEmpty()) {
            return new SurveySummaryResponse(0, 0, 0, 0, 0);
        }
        double avgContent = all.stream().mapToInt(SurveyResponse::getContentRating).average().orElse(0);
        double avgTeacher = all.stream().mapToInt(SurveyResponse::getTeacherRating).average().orElse(0);
        double avgMaterial = all.stream().mapToInt(SurveyResponse::getMaterialRating).average().orElse(0);
        double avgOverall = all.stream().mapToInt(SurveyResponse::getOverallRating).average().orElse(0);
        return new SurveySummaryResponse(all.size(), round1(avgContent), round1(avgTeacher), round1(avgMaterial), round1(avgOverall));
    }

    @Transactional(readOnly = true)
    public List<SurveyResponseDetail> getAllResponsesForAdmin() {
        return surveyResponseRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> new SurveyResponseDetail(
                        r.getId(),
                        r.isAnonymous() ? null : r.getUsername(),
                        r.getCourseName(),
                        r.getContentRating(), r.getTeacherRating(), r.getMaterialRating(), r.getOverallRating(),
                        r.getComment(), r.isAnonymous(), r.getCreatedAt().format(DATETIME_FORMAT)
                ))
                .toList();
    }

    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalStateException("별점은 1~5 사이여야 해요.");
        }
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}