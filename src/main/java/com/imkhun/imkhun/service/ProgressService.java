package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.StudentLevelRecord;
import com.imkhun.imkhun.domain.VocabularySet;
import com.imkhun.imkhun.domain.VocabularyWord;
import com.imkhun.imkhun.dto.CourseProgressResponse;
import com.imkhun.imkhun.repository.StudentLevelRecordRepository;
import com.imkhun.imkhun.repository.VocabularyProgressRepository;
import com.imkhun.imkhun.repository.VocabularySetRepository;
import com.imkhun.imkhun.repository.VocabularyWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 학생 마이페이지 "나의 성장"에서 보여줄 언어별 진도율 — "현재 레벨 + 그 언어 단어장 중 외운 비율(%)"을
// 한 줄로 모아줌. 같은 언어로 신청한 강의가 여러 개여도 언어 하나당 한 줄로 합쳐서 보여줌.
@Service
public class ProgressService {

    private final StudentAuthService studentAuthService;
    private final ApplicationService applicationService;
    private final StudentLevelRecordRepository studentLevelRecordRepository;
    private final VocabularySetRepository vocabularySetRepository;
    private final VocabularyWordRepository vocabularyWordRepository;
    private final VocabularyProgressRepository vocabularyProgressRepository;

    public ProgressService(StudentAuthService studentAuthService, ApplicationService applicationService,
                           StudentLevelRecordRepository studentLevelRecordRepository,
                           VocabularySetRepository vocabularySetRepository,
                           VocabularyWordRepository vocabularyWordRepository,
                           VocabularyProgressRepository vocabularyProgressRepository) {
        this.studentAuthService = studentAuthService;
        this.applicationService = applicationService;
        this.studentLevelRecordRepository = studentLevelRecordRepository;
        this.vocabularySetRepository = vocabularySetRepository;
        this.vocabularyWordRepository = vocabularyWordRepository;
        this.vocabularyProgressRepository = vocabularyProgressRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseProgressResponse> getProgressForStudent(String username) {
        List<Application> approved = studentAuthService.getApprovedApplications(username);

        // 언어 코드 하나당 그 언어로 신청한 Application들을 모아줌 (순서 유지)
        Map<String, List<Application>> byLanguage = new LinkedHashMap<>();
        for (Application a : approved) {
            String lang = applicationService.extractLanguageCode(a.getCourseName());
            byLanguage.computeIfAbsent(lang, k -> new ArrayList<>()).add(a);
        }

        // 내가 "외웠어요"로 표시해둔 단어 id들을 미리 한 번에 모아둠
        Set<Long> myLearnedWordIds = vocabularyProgressRepository.findByUsername(username).stream()
                .filter(p -> p.isLearned())
                .map(p -> p.getWordId())
                .collect(Collectors.toSet());

        List<CourseProgressResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<Application>> entry : byLanguage.entrySet()) {
            String language = entry.getKey();
            List<Application> apps = entry.getValue();

            String courseNames = apps.stream().map(Application::getCourseName).distinct()
                    .collect(Collectors.joining(" / "));

            // 이 언어 소속 Application들 중 가장 최근에 기록된 레벨을 대표로 보여줌
            String currentLevel = apps.stream()
                    .flatMap(a -> studentLevelRecordRepository
                            .findByApplicationIdOrderByRecordedDateDescCreatedAtDesc(a.getId()).stream())
                    .max((a, b) -> {
                        int byDate = a.getRecordedDate().compareTo(b.getRecordedDate());
                        return byDate != 0 ? byDate : a.getCreatedAt().compareTo(b.getCreatedAt());
                    })
                    .map(StudentLevelRecord::getLevel)
                    .orElse(null);

            long totalWords = 0;
            long learnedWords = 0;
            // "computer"/"other"는 단어장 체계가 없어서 진도율 계산 대상이 아님
            if (!"computer".equals(language) && !"other".equals(language)) {
                for (VocabularySet set : vocabularySetRepository.findByLanguageOrderByCreatedAtAsc(language)) {
                    List<VocabularyWord> words = vocabularyWordRepository.findBySetIdOrderByCreatedAtAsc(set.getId());
                    totalWords += words.size();
                    learnedWords += words.stream().filter(w -> myLearnedWordIds.contains(w.getId())).count();
                }
            }

            Integer percent = totalWords > 0 ? (int) Math.round(learnedWords * 100.0 / totalWords) : null;
            result.add(new CourseProgressResponse(language, courseNames, currentLevel, learnedWords, totalWords, percent));
        }

        return result;
    }
}