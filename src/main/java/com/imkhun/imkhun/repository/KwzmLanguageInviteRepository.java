package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.KwzmLanguageInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KwzmLanguageInviteRepository extends JpaRepository<KwzmLanguageInvite, Long> {

    List<KwzmLanguageInvite> findByLanguageAndContentTypeOrderByInvitedAtDesc(String language, String contentType);

    Optional<KwzmLanguageInvite> findByLanguageAndContentTypeAndStudentNumber(String language, String contentType, String studentNumber);

    boolean existsByLanguageAndContentTypeAndStudentNumber(String language, String contentType, String studentNumber);
}