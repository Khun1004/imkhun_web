package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

    Optional<PostReaction> findByPostIdAndUsername(Long postId, String username);

    long countByPostIdAndType(Long postId, String type);
}