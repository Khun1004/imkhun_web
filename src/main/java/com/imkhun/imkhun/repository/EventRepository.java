package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByOrderByEventDateDesc();
}