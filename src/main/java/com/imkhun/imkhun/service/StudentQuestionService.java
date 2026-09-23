package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.StudentQuestion;
import com.imkhun.imkhun.dto.*;
import com.imkhun.imkhun.repository.StudentQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StudentQuestionService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StudentQuestionRepository studentQuestionRepository;
    private final NotificationService notificationService;

    public StudentQuestionService(StudentQuestionRepository studentQuestionRepository, NotificationService notificationService) {
        this.studentQuestionRepository = studentQuestionRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public StudentQuestionResponse createQuestion(String username, CreateStudentQuestionRequest request) {
        if (request.questionText() == null || request.questionText().isBlank()) {
            throw new IllegalStateException("질문 내용을 입력해주세요.");
        }
        StudentQuestion saved = studentQuestionRepository.save(StudentQuestion.create(username, request.questionText()));
        notificationService.notifyAdmin("STUDENT_QUESTION", "학생이 익명으로 질문을 보냈어요.", null);
        return toStudentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StudentQuestionResponse> getMyQuestions(String username) {
        return studentQuestionRepository.findByUsernameOrderByCreatedAtDesc(username).stream()
                .map(this::toStudentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminQuestionResponse> getAllQuestionsForAdmin() {
        return studentQuestionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public AdminQuestionResponse answerQuestion(Long id, AnswerStudentQuestionRequest request) {
        if (request.answerText() == null || request.answerText().isBlank()) {
            throw new IllegalStateException("답변 내용을 입력해주세요.");
        }
        StudentQuestion question = studentQuestionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("질문을 찾을 수 없어요."));
        question.addAnswer(request.answerText());
        StudentQuestion saved = studentQuestionRepository.save(question);
        notificationService.notifyStudent(question.getUsername(), "QUESTION_ANSWERED", "보내주신 질문에 선생님이 답변을 남겼어요.", null);
        return toAdminResponse(saved);
    }

    private StudentQuestionResponse toStudentResponse(StudentQuestion q) {
        return new StudentQuestionResponse(q.getId(), q.getQuestionText(), q.getAnswerText(),
                q.getCreatedAt().format(DATETIME_FORMAT), q.getAnsweredAt() != null ? q.getAnsweredAt().format(DATETIME_FORMAT) : null);
    }

    private AdminQuestionResponse toAdminResponse(StudentQuestion q) {
        return new AdminQuestionResponse(q.getId(), q.getQuestionText(), q.getAnswerText(),
                q.getCreatedAt().format(DATETIME_FORMAT), q.getAnsweredAt() != null ? q.getAnsweredAt().format(DATETIME_FORMAT) : null);
    }
}