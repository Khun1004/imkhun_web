package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.dto.FaqResponse;
import com.imkhun.imkhun.service.FaqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faqs")
public class FaqController {

    private final FaqService faqService;

    public FaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    // FAQ는 로그인 안 해도 누구나 볼 수 있음
    @GetMapping
    public ResponseEntity<List<FaqResponse>> getFaqs() {
        return ResponseEntity.ok(faqService.getAllFaqs());
    }
}