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

    List<Application> findByPaymentConfirmedByStudentAtIsNotNullAndPaymentConfirmedByAdminAtIsNull();

    List<Application> findByPaymentConfirmedByAdminAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    // 출석 체크용 — 요일 필터링은 서비스 쪽에서 classDays 문자열을 보고 걸러줌
    List<Application> findByStatusAndClassDaysIsNotNull(String status);

    List<Application> findByStatus(String status);

    // 결제 리마인더용 — 승인됐고, 학생이 아직 결제 확인을 안 한 신청들
    List<Application> findByStatusAndPaymentConfirmedByStudentAtIsNull(String status);
}