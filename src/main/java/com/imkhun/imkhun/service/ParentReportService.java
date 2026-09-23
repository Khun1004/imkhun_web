package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.ParentReportLink;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.GrowthReportResponse;
import com.imkhun.imkhun.dto.ParentReportLinkResponse;
import com.imkhun.imkhun.dto.ParentReportResponse;
import com.imkhun.imkhun.repository.ParentReportLinkRepository;
import com.imkhun.imkhun.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ParentReportService {

    private final ParentReportLinkRepository parentReportLinkRepository;
    private final UserRepository userRepository;
    private final GrowthReportService growthReportService;
    private final AttendanceStreakService attendanceStreakService;

    public ParentReportService(ParentReportLinkRepository parentReportLinkRepository, UserRepository userRepository,
                               GrowthReportService growthReportService, AttendanceStreakService attendanceStreakService) {
        this.parentReportLinkRepository = parentReportLinkRepository;
        this.userRepository = userRepository;
        this.growthReportService = growthReportService;
        this.attendanceStreakService = attendanceStreakService;
    }

    // 이미 링크가 있으면 그대로 돌려주고, 없으면 새로 만듦 (마이페이지 들어갈 때마다 새로 안 바뀌게)
    @Transactional
    public ParentReportLinkResponse getOrCreateLink(String username) {
        ParentReportLink link = parentReportLinkRepository.findByUsername(username)
                .orElseGet(() -> parentReportLinkRepository.save(ParentReportLink.create(generateToken(), username)));
        return toLinkResponse(link);
    }

    // 기존 링크를 무효화하고 새 링크를 만듦 (예전에 보낸 링크로는 더 이상 못 들어오게 하고 싶을 때)
    @Transactional
    public ParentReportLinkResponse regenerateLink(String username) {
        parentReportLinkRepository.deleteByUsername(username);
        ParentReportLink link = parentReportLinkRepository.save(ParentReportLink.create(generateToken(), username));
        return toLinkResponse(link);
    }

    @Transactional(readOnly = true)
    public ParentReportResponse getReportByToken(String token) {
        ParentReportLink link = parentReportLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("링크가 만료됐거나 잘못됐어요."));

        String username = link.getUsername();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalStateException("학생을 찾을 수 없어요."));
        GrowthReportResponse growth = growthReportService.getReport(username);
        int streak = attendanceStreakService.getStreakForStudent(username).currentStreak();

        return new ParentReportResponse(user.getNickname(), streak, growth.attendanceRate(), growth.totalAttendanceRecords(),
                growth.assignmentCompletionRate(), growth.totalAssignments(), growth.completedAssignments(), growth.levelHistory());
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private ParentReportLinkResponse toLinkResponse(ParentReportLink link) {
        return new ParentReportLinkResponse(link.getToken(), "/kwzmcenter/parent-report.html?token=" + link.getToken());
    }
}