package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.dto.NoticeResponse;
import com.imkhun.imkhun.service.NoticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    // 공지 목록은 로그인 안 해도 누구나 볼 수 있음 (예약 시간이 안 된 글은 안 보임)
    @GetMapping
    public ResponseEntity<List<NoticeResponse>> getNotices() {
        return ResponseEntity.ok(noticeService.getPublishedNotices());
    }
}