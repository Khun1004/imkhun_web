package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.VocabularyQuizResult;
import com.imkhun.imkhun.dto.LeaderboardEntryResponse;
import com.imkhun.imkhun.dto.LeaderboardResponse;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.VocabularyQuizResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

// 랭킹보드 — 이름은 절대 안 보여주고 등수/점수만 보여줌 (익명). 학생 본인 줄만 화면에서 강조 표시함
@Service
public class LeaderboardService {

    private static final int TOP_COUNT = 10;

    private final ApplicationRepository applicationRepository;
    private final AttendanceStreakService attendanceStreakService;
    private final VocabularyQuizResultRepository vocabularyQuizResultRepository;

    public LeaderboardService(ApplicationRepository applicationRepository, AttendanceStreakService attendanceStreakService,
                              VocabularyQuizResultRepository vocabularyQuizResultRepository) {
        this.applicationRepository = applicationRepository;
        this.attendanceStreakService = attendanceStreakService;
        this.vocabularyQuizResultRepository = vocabularyQuizResultRepository;
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getAttendanceStreakLeaderboard(String currentUsername) {
        List<String> usernames = distinctApprovedUsernames();
        Map<String, Integer> valueByUsername = new HashMap<>();
        for (String username : usernames) {
            valueByUsername.put(username, attendanceStreakService.getStreakForStudent(username).currentStreak());
        }
        return buildResponse(valueByUsername, currentUsername);
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getVocabLeaderboard(String currentUsername) {
        List<String> usernames = distinctApprovedUsernames();
        Map<String, Integer> valueByUsername = new HashMap<>();
        for (String username : usernames) {
            valueByUsername.put(username, 0);
        }
        for (VocabularyQuizResult result : vocabularyQuizResultRepository.findAll()) {
            valueByUsername.merge(result.getUsername(), result.getScore(), Integer::sum);
        }
        return buildResponse(valueByUsername, currentUsername);
    }

    private List<String> distinctApprovedUsernames() {
        return applicationRepository.findByStatus("APPROVED").stream()
                .map(Application::getUsername)
                .distinct()
                .toList();
    }

    private LeaderboardResponse buildResponse(Map<String, Integer> valueByUsername, String currentUsername) {
        List<Map.Entry<String, Integer>> sorted = valueByUsername.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .collect(Collectors.toList());

        List<LeaderboardEntryResponse> top = new ArrayList<>();
        int myRank = 0;
        int myValue = valueByUsername.getOrDefault(currentUsername, 0);

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            int rank = i + 1;
            boolean isMe = entry.getKey().equals(currentUsername);
            if (isMe) myRank = rank;
            if (i < TOP_COUNT) {
                top.add(new LeaderboardEntryResponse(rank, entry.getValue(), isMe));
            }
        }

        if (myRank == 0 && !valueByUsername.containsKey(currentUsername)) {
            // 승인된 수강 신청이 없는 등, 리더보드에 아예 안 잡히는 경우
            myRank = sorted.size() + 1;
        }

        return new LeaderboardResponse(top, myRank, myValue, sorted.size());
    }
}