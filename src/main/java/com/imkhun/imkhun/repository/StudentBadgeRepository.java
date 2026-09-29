package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudentBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentBadgeRepository extends JpaRepository<StudentBadge, Long> {

    List<StudentBadge> findByUsername(String username);

    Optional<StudentBadge> findByUsernameAndBadgeKey(String username, String badgeKey);
}