package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUsernameOrderByCreatedAtDesc(String username);

    List<Application> findAllByOrderByCreatedAtDesc();

    long countByStudentNumberStartingWith(String prefix);

    Optional<Application> findByUsernameAndStudentNumberAndStatus(String username, String studentNumber, String status);

    Optional<Application> findByStudentNumber(String studentNumber);

    long countByStatus(String status);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countByPaymentConfirmedByStudentAtIsNotNullAndPaymentConfirmedByAdminAtIsNull();

    // 출석 체크용 — 요일 필터링은 서비스 쪽에서 classDays 문자열을 보고 걸러줌
    List<Application> findByStatusAndClassDaysIsNotNull(String status);
}