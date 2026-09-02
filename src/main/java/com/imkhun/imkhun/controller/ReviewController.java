package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.dto.CreateReviewRequest;
import com.imkhun.imkhun.dto.ReviewResponse;
import com.imkhun.imkhun.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // 리뷰 목록은 로그인 안 해도 누구나 볼 수 있음
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    // 로그인한 본인이 쓴 리뷰만 (마이페이지 - 리뷰 내역)
    @GetMapping("/mine")
    public ResponseEntity<?> getMyReviews(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(reviewService.getMyReviews(authentication.getName()));
    }

    // 리뷰 작성은 로그인한 사용자만 가능
    @PostMapping
    public ResponseEntity<?> createReview(Authentication authentication,
                                          @RequestBody CreateReviewRequest request) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body("로그인이 필요해요.");
        }

        try {
            ReviewResponse response = reviewService.createReview(authentication.getName(), request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}