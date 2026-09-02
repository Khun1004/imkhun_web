package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Review;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.CreateReviewRequest;
import com.imkhun.imkhun.dto.ReviewResponse;
import com.imkhun.imkhun.repository.ReviewRepository;
import com.imkhun.imkhun.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    public ReviewResponse createReview(String username, CreateReviewRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        if (request.rating() < 1 || request.rating() > 5) {
            throw new IllegalStateException("별점은 1~5 사이여야 합니다.");
        }
        if (request.courseName() == null || request.courseName().isBlank()
                || request.content() == null || request.content().isBlank()) {
            throw new IllegalStateException("과목과 후기 내용을 모두 입력해주세요.");
        }

        Review review = Review.create(
                user.getUsername(),
                user.getNickname(),
                request.courseName(),
                request.rating(),
                request.content()
        );
        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 로그인한 사용자 본인이 쓴 리뷰만 (마이페이지 - 리뷰 내역)
    public List<ReviewResponse> getMyReviews(String username) {
        return reviewRepository.findByUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 관리자가 리뷰에 답글 남기기
    public ReviewResponse replyToReview(Long reviewId, String reply) {
        if (reply == null || reply.isBlank()) {
            throw new IllegalStateException("답글 내용을 입력해주세요.");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalStateException("리뷰를 찾을 수 없어요."));
        review.updateReply(reply);
        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    private ReviewResponse toResponse(Review review) {
        // 프로필 사진은 스냅샷이 아니라 항상 "현재" 사진을 보여주기 위해 매번 조회
        Optional<User> author = userRepository.findByUsername(review.getUsername());
        String profileImage = author.map(User::getProfileImage).orElse(null);

        return new ReviewResponse(
                review.getId(),
                review.getNickname(),
                profileImage,
                review.getCourseName(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt().format(DATE_FORMAT),
                review.getAdminReply(),
                review.getRepliedAt() != null ? review.getRepliedAt().format(DATE_FORMAT) : null
        );
    }
}