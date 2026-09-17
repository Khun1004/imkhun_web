package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Notice;
import com.imkhun.imkhun.dto.CreateNoticeRequest;
import com.imkhun.imkhun.dto.NoticeResponse;
import com.imkhun.imkhun.repository.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATETIME_INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    // 관리자용 — 예약 대기중인 것까지 전부 다 보여줌
    @Transactional(readOnly = true)
    public List<NoticeResponse> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 공개 홈페이지용 — 아직 예약 시간이 안 된 글은 빼고 보여줌
    @Transactional(readOnly = true)
    public List<NoticeResponse> getPublishedNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(Notice::isPublishedNow)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NoticeResponse createNotice(CreateNoticeRequest request) {
        validate(request);
        LocalDateTime scheduledAt = parseScheduledAt(request.scheduledAt());
        Notice saved = noticeRepository.save(Notice.create(request.title(), request.content(), scheduledAt));
        return toResponse(saved);
    }

    @Transactional
    public NoticeResponse updateNotice(Long id, CreateNoticeRequest request) {
        validate(request);
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("공지를 찾을 수 없어요."));
        notice.update(request.title(), request.content(), parseScheduledAt(request.scheduledAt()));
        return toResponse(noticeRepository.save(notice));
    }

    @Transactional
    public void deleteNotice(Long id) {
        if (!noticeRepository.existsById(id)) {
            throw new IllegalStateException("공지를 찾을 수 없어요.");
        }
        noticeRepository.deleteById(id);
    }

    private LocalDateTime parseScheduledAt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, DATETIME_INPUT_FORMAT);
        } catch (Exception e) {
            throw new IllegalStateException("예약 시간 형식이 올바르지 않아요.");
        }
    }

    private void validate(CreateNoticeRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalStateException("제목을 입력해주세요.");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalStateException("내용을 입력해주세요.");
        }
    }

    private NoticeResponse toResponse(Notice notice) {
        return new NoticeResponse(
                notice.getId(), notice.getTitle(), notice.getContent(),
                notice.getCreatedAt().format(DATE_FORMAT),
                notice.getScheduledAt() != null ? notice.getScheduledAt().format(DATETIME_INPUT_FORMAT) : null,
                notice.isPublishedNow()
        );
    }
}