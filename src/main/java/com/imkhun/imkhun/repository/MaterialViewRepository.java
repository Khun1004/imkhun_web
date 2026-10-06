package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.MaterialView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialViewRepository extends JpaRepository<MaterialView, Long> {

    List<MaterialView> findByMaterialIdOrderByViewedAtDesc(Long materialId);

    Optional<MaterialView> findByMaterialIdAndUsername(Long materialId, String username);

    long countByMaterialId(Long materialId);
}