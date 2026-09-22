package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Event;
import com.imkhun.imkhun.dto.CreateEventRequest;
import com.imkhun.imkhun.dto.EventResponse;
import com.imkhun.imkhun.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EventService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalStateException("제목을 입력해주세요.");
        }
        if (request.eventDate() == null || request.eventDate().isBlank()) {
            throw new IllegalStateException("행사 날짜를 입력해주세요.");
        }

        LocalDate eventDate;
        try {
            eventDate = LocalDate.parse(request.eventDate());
        } catch (Exception e) {
            throw new IllegalStateException("날짜 형식이 올바르지 않아요.");
        }

        Event saved = eventRepository.save(Event.create(request.title(), request.content(), eventDate));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAllByOrderByEventDateDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(event.getId(), event.getTitle(), event.getContent(), event.getEventDate().format(DATE_FORMAT));
    }
}