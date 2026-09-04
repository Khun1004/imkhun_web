package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    Optional<CommentReaction> findByCommentIdAndUsername(Long commentId, String username);

    long countByCommentIdAndType(Long commentId, String type);
}