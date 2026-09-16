package com.imkhun.imkhun.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// 하루가 끝난 뒤, 그날 수업이 있었는데 아무 기록도 안 남은(미체크) 학생을 자동으로 결석 처리함.
// 자정 직후가 아니라 00:30에 도는 이유: 23시대 수업까지 체크 가능 시간(수업 시작 1시간 후)이 다 끝난 뒤에 돌려야
// "아직 체크할 시간이 남았는데 결석 처리됐어요" 같은 일이 안 생겨서요.
@Service
public class AutoAbsentService {

    private final AttendanceService attendanceService;

    public AutoAbsentService(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void markYesterdayUncheckedAsAbsent() {
        attendanceService.markRestAbsent(LocalDate.now().minusDays(1));
    }
}