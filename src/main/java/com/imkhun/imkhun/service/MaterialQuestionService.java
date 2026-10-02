package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.MaterialQuestion;
import com.imkhun.imkhun.domain.StudyMaterial;
import com.imkhun.imkhun.dto.AnswerStudentQuestionRequest;
import com.imkhun.imkhun.dto.CreateMaterialQuestionRequest;
import com.imkhun.imkhun.dto.MaterialQuestionResponse;
import com.imkhun.imkhun.repository.MaterialQuestionRepository;
import com.imkhun.imkhun.repository.StudyMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MaterialQuestionService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MaterialQuestionRepository materialQuestionRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final NotificationService notificationService;

    public MaterialQuestionService(MaterialQuestionRepository materialQuestionRepository,
                                   StudyMaterialRepository studyMaterialRepository,
                                   NotificationService notificationService) {
        this.materialQuestionRepository = materialQuestionRepository;
        this.studyMaterialRepository = studyMaterialRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public MaterialQuestionResponse askQuestion(Long materialId, String username, String nickname, CreateMaterialQuestionRequest request) {
        if (request.questionText() == null || request.questionText().isBlank()) {
            throw new IllegalStateException("질문 내용을 입력해주세요.");
        }
        StudyMaterial material = studyMaterialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalStateException("자료를 찾을 수 없어요."));

        MaterialQuestion saved = materialQuestionRepository.save(
                MaterialQuestion.create(materialId, username, nickname, request.questionText()));
        notificationService.notifyAdmin("MATERIAL_QUESTION",
                nickname + "님이 \"" + material.getTitle() + "\" 자료에 질문을 남겼어요.", null);
        return toResponse(saved, username);
    }

    @Transactional(readOnly = true)
    public List<MaterialQuestionResponse> getQuestionsForMaterial(Long materialId, String currentUsername) {
        return materialQuestionRepository.findByMaterialIdOrderByCreatedAtAsc(materialId).stream()
                .map(q -> toResponse(q, currentUsername))
                .toList();
    }

    // 관리자 화면에서는 "내 질문"이라는 개념이 없어서 currentUsername 없이 그대로 보여줌
    @Transactional(readOnly = true)
    public List<MaterialQuestionResponse> getQuestionsForMaterialForAdmin(Long materialId) {
        return materialQuestionRepository.findByMaterialIdOrderByCreatedAtAsc(materialId).stream()
                .map(q -> toResponse(q, null))
                .toList();
    }

    @Transactional
    public MaterialQuestionResponse answerQuestion(Long questionId, AnswerStudentQuestionRequest request) {
        if (request.answerText() == null || request.answerText().isBlank()) {
            throw new IllegalStateException("답변 내용을 입력해주세요.");
        }
        MaterialQuestion question = materialQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalStateException("질문을 찾을 수 없어요."));
        question.addAnswer(request.answerText());
        MaterialQuestion saved = materialQuestionRepository.save(question);

        try {
            notificationService.notifyStudent(question.getUsername(), "MATERIAL_QUESTION_ANSWERED",
                    "자료에 남기신 질문에 선생님이 답변을 남겼어요.", null);
        } catch (Exception e) {
            // 알림이 실패해도 답변 저장 자체는 반드시 남도록 함
            System.err.println("[MaterialQuestionService] 답변 알림 처리 중 오류: " + e);
        }
        return toResponse(saved, null);
    }

    // 관리자 자료 목록에서 자료마다 "답변 안 한 질문 개수" 배지를 보여주기 위한 집계
    @Transactional(readOnly = true)
    public Map<Long, Long> getUnansweredCounts(List<Long> materialIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (materialIds == null || materialIds.isEmpty()) return counts;
        for (Object[] row : materialQuestionRepository.countUnansweredGroupedByMaterialId(materialIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private MaterialQuestionResponse toResponse(MaterialQuestion q, String currentUsername) {
        boolean mine = currentUsername != null && currentUsername.equals(q.getUsername());
        return new MaterialQuestionResponse(q.getId(), q.getMaterialId(), q.getNickname(), q.getQuestionText(),
                q.getAnswerText(), q.getCreatedAt().format(DATETIME_FORMAT),
                q.getAnsweredAt() != null ? q.getAnsweredAt().format(DATETIME_FORMAT) : null, mine);
    }
}