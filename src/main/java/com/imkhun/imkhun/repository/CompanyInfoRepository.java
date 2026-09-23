package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.CompanyInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyInfoRepository extends JpaRepository<CompanyInfo, Long> {

    Optional<CompanyInfo> findByType(String type);
}