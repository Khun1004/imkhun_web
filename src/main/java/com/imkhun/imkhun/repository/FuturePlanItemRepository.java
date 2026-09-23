package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.FuturePlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuturePlanItemRepository extends JpaRepository<FuturePlanItem, Long> {

    List<FuturePlanItem> findAllByOrderBySortOrderAsc();
}