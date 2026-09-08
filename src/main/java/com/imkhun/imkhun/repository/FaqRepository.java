package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findAllByOrderByCreatedAtAsc();
}