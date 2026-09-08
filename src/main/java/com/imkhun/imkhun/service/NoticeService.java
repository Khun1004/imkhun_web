package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Notice;
import com.imkhun.imkhun.dto.CreateNoticeRequest;
import com.imkhun.imkhun.dto.NoticeResponse;
import com.imkhun.imkhun.repository.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NoticeResponse createNotice(CreateNoticeRequest request) {
        validate(request);
        Notice saved = noticeRepository.save(Notice.create(request.title(), request.content()));
        return toResponse(saved);
    }

    @Transactional
    public NoticeResponse updateNotice(Long id, CreateNoticeRequest request) {
        validate(request);
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("공지를 찾을 수 없어요."));
        notice.update(request.title(), request.content());
        return toResponse(noticeRepository.save(notice));
    }

    @Transactional
    public void deleteNotice(Long id) {
        if (!noticeRepository.existsById(id)) {
            throw new IllegalStateException("공지를 찾을 수 없어요.");
        }
        noticeRepository.deleteById(id);
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
        return new NoticeResponse(notice.getId(), notice.getTitle(), notice.getContent(),
                notice.getCreatedAt().format(DATE_FORMAT));
    }
}