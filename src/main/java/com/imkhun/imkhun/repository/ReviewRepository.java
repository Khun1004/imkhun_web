package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByOrderByCreatedAtDesc();

    List<Review> findByUsernameOrderByCreatedAtDesc(String username);
}