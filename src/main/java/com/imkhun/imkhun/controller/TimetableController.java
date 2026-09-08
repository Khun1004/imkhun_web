package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.dto.TimetableEntryResponse;
import com.imkhun.imkhun.service.TimetableService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {

    private final TimetableService timetableService;

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    // 시간표는 로그인 안 해도 누구나 볼 수 있음
    @GetMapping
    public ResponseEntity<List<TimetableEntryResponse>> getTimetable() {
        return ResponseEntity.ok(timetableService.getAllEntries());
    }
}