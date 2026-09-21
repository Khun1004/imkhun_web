package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.Assignment;
import com.imkhun.imkhun.domain.AssignmentSubmission;
import com.imkhun.imkhun.dto.AssignmentSubmissionResponse;
import com.imkhun.imkhun.dto.AttachmentItem;
import com.imkhun.imkhun.dto.CreateSubmissionRequest;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.AssignmentRepository;
import com.imkhun.imkhun.repository.AssignmentSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
public class AssignmentSubmissionService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public AssignmentSubmissionService(AssignmentSubmissionRepository assignmentSubmissionRepository,
                                       AssignmentRepository assignmentRepository, ApplicationRepository applicationRepository,
                                       NotificationService notificationService) {
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void submit(Long assignmentId, String username, CreateSubmissionRequest request) {
        List<AttachmentItem> attachments = request.attachments() != null ? request.attachments() : Collections.emptyList();
        if ((request.textAnswer() == null || request.textAnswer().isBlank()) && attachments.isEmpty()) {
            throw new IllegalStateException("답을 적거나 파일을 첨부해주세요.");
        }
        if (attachments.size() > 5) {
            throw new IllegalStateException("첨부파일은 최대 5개까지만 가능해요.");
        }

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("숙제를 찾을 수 없어요."));
        Application application = applicationRepository.findById(assignment.getApplicationId())
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        if (!application.getUsername().equals(username)) {
            throw new IllegalStateException("본인의 숙제에만 제출할 수 있어요.");
        }

        String attachmentsJson = attachments.isEmpty() ? null : toJson(attachments);

        AssignmentSubmission submission = assignmentSubmissionRepository.findByAssignmentId(assignmentId)
                .orElseGet(() -> AssignmentSubmission.create(assignmentId, username, null, null));
        submission.updateContent(request.textAnswer(), attachmentsJson);
        assignmentSubmissionRepository.save(submission);

        // 제출하면 자동으로 완료 처리
        assignment.markComplete();
        assignmentRepository.save(assignment);

        notificationService.notifyAdmin("ASSIGNMENT_SUBMITTED",
                username + "님이 \"" + assignment.getTitle() + "\" 숙제를 제출했어요.", null);
    }

    @Transactional(readOnly = true)
    public AssignmentSubmissionResponse getForStudent(Long assignmentId, String username) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        Application application = assignment != null ? applicationRepository.findById(assignment.getApplicationId()).orElse(null) : null;

        return assignmentSubmissionRepository.findByAssignmentId(assignmentId)
                .filter(s -> s.getUsername().equals(username))
                .map(s -> toResponse(s, assignment, application))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getAllForAdmin() {
        return assignmentSubmissionRepository.findAllByOrderBySubmittedAtDesc().stream()
                .map(s -> {
                    Assignment assignment = assignmentRepository.findById(s.getAssignmentId()).orElse(null);
                    Application application = assignment != null
                            ? applicationRepository.findById(assignment.getApplicationId()).orElse(null) : null;
                    return toResponse(s, assignment, application);
                })
                .toList();
    }

    @Transactional
    public void addComment(Long submissionId, String comment) {
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalStateException("제출 내역을 찾을 수 없어요."));
        submission.addComment(comment);
        assignmentSubmissionRepository.save(submission);

        notificationService.notifyStudent(submission.getUsername(), "ASSIGNMENT_COMMENT_ADDED",
                "숙제 제출물에 선생님이 코멘트를 남겼어요.", null);
    }

    private AssignmentSubmissionResponse toResponse(AssignmentSubmission submission, Assignment assignment, Application application) {
        return new AssignmentSubmissionResponse(
                submission.getId(), submission.getAssignmentId(), submission.getUsername(),
                assignment != null ? assignment.getTitle() : "-",
                application != null ? application.getCourseName() : "-",
                submission.getTextAnswer(), fromJson(submission.getAttachmentsJson()),
                submission.getAdminComment(), submission.getSubmittedAt().format(DATETIME_FORMAT)
        );
    }

    // Jackson 같은 외부 라이브러리 없이, 이 간단한 구조(data/name 두 필드짜리 배열)만
    // 다루는 아주 작은 JSON 인코더/디코더를 직접 만들어서 씀
    private String toJson(List<AttachmentItem> attachments) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attachments.size(); i++) {
            if (i > 0) sb.append(",");
            AttachmentItem item = attachments.get(i);
            sb.append("{\"data\":\"").append(escapeJson(item.data())).append("\",\"name\":\"").append(escapeJson(item.name())).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<AttachmentItem> fromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<AttachmentItem> result = new java.util.ArrayList<>();
            int i = 0;
            while (i < json.length()) {
                int dataKey = json.indexOf("\"data\":\"", i);
                if (dataKey == -1) break;
                int dataStart = dataKey + 8;
                int dataEnd = findUnescapedQuote(json, dataStart);
                String data = unescapeJson(json.substring(dataStart, dataEnd));

                int nameKey = json.indexOf("\"name\":\"", dataEnd);
                int nameStart = nameKey + 8;
                int nameEnd = findUnescapedQuote(json, nameStart);
                String name = unescapeJson(json.substring(nameStart, nameEnd));

                result.add(new AttachmentItem(data, name));
                i = nameEnd + 1;
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private int findUnescapedQuote(String s, int from) {
        int i = from;
        while (i < s.length()) {
            if (s.charAt(i) == '"' && s.charAt(i - 1) != '\\') return i;
            i++;
        }
        return s.length();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private String unescapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"' -> { sb.append('"'); i += 2; }
                    case '\\' -> { sb.append('\\'); i += 2; }
                    case 'n' -> { sb.append('\n'); i += 2; }
                    case 'r' -> { sb.append('\r'); i += 2; }
                    case 't' -> { sb.append('\t'); i += 2; }
                    case 'u' -> {
                        if (i + 6 <= s.length()) {
                            sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                            i += 6;
                        } else {
                            sb.append(c);
                            i++;
                        }
                    }
                    default -> { sb.append(next); i += 2; }
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}