package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.KwzmLanguageInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KwzmLanguageInviteRepository extends JpaRepository<KwzmLanguageInvite, Long> {

    List<KwzmLanguageInvite> findByLanguageOrderByInvitedAtDesc(String language);

    Optional<KwzmLanguageInvite> findByLanguageAndStudentNumber(String language, String studentNumber);

    boolean existsByLanguageAndStudentNumber(String language, String studentNumber);
}