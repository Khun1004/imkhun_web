package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.ParentReportLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParentReportLinkRepository extends JpaRepository<ParentReportLink, Long> {

    Optional<ParentReportLink> findByToken(String token);

    Optional<ParentReportLink> findByUsername(String username);

    void deleteByUsername(String username);
}