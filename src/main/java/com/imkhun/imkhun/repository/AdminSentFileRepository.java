package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.AdminSentFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AdminSentFileRepository extends JpaRepository<AdminSentFile, Long> {

    List<AdminSentFile> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);

    // 학생이 본인의 모든 신청(강의)에 대해 받은 파일을 한 번에 조회할 때 씀
    List<AdminSentFile> findByApplicationIdInOrderByCreatedAtDesc(Collection<Long> applicationIds);
}