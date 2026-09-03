package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUsernameOrderByCreatedAtDesc(String username);

    List<Application> findAllByOrderByCreatedAtDesc();

    long countByStudentNumberStartingWith(String prefix);

    Optional<Application> findByUsernameAndStudentNumberAndStatus(String username, String studentNumber, String status);

    Optional<Application> findByStudentNumber(String studentNumber);
}