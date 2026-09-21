package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.VocabularySet;
import com.imkhun.imkhun.domain.VocabularyWord;
import com.imkhun.imkhun.dto.*;
import com.imkhun.imkhun.repository.VocabularyQuizResultRepository;
import com.imkhun.imkhun.repository.VocabularySetRepository;
import com.imkhun.imkhun.repository.VocabularyWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class VocabularyService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final VocabularySetRepository vocabularySetRepository;
    private final VocabularyWordRepository vocabularyWordRepository;
    private final VocabularyQuizResultRepository vocabularyQuizResultRepository;

    public VocabularyService(VocabularySetRepository vocabularySetRepository, VocabularyWordRepository vocabularyWordRepository,
                             VocabularyQuizResultRepository vocabularyQuizResultRepository) {
        this.vocabularySetRepository = vocabularySetRepository;
        this.vocabularyWordRepository = vocabularyWordRepository;
        this.vocabularyQuizResultRepository = vocabularyQuizResultRepository;
    }

    // ---------- Part(세트) 관리 ----------

    @Transactional
    public VocabularySetResponse createSet(CreateVocabularySetRequest request) {
        if (request.language() == null || request.language().isBlank()
                || request.name() == null || request.name().isBlank()
                || request.category() == null || request.category().isBlank()) {
            throw new IllegalStateException("언어, Part 이름, 종류를 모두 입력해주세요.");
        }
        if (request.quizTimeLimitMinutes() <= 0) {
            throw new IllegalStateException("퀴즈 제한시간은 1분 이상이어야 해요.");
        }
        VocabularySet saved = vocabularySetRepository.save(
                VocabularySet.create(request.language(), request.name(), request.category(), request.quizTimeLimitMinutes()));
        return toSetResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<VocabularySetResponse> getSetsForLanguage(String language) {
        return vocabularySetRepository.findByLanguageOrderByCreatedAtAsc(language).stream()
                .map(this::toSetResponse)
                .toList();
    }

    @Transactional
    public void deleteSet(Long setId) {
        vocabularyWordRepository.deleteBySetId(setId);
        vocabularyQuizResultRepository.deleteBySetId(setId);
        vocabularySetRepository.deleteById(setId);
    }

    private VocabularySetResponse toSetResponse(VocabularySet set) {
        long wordCount = vocabularyWordRepository.countBySetId(set.getId());
        return new VocabularySetResponse(set.getId(), set.getLanguage(), set.getName(), set.getCategory(),
                set.getQuizTimeLimitMinutes(), wordCount);
    }

    // ---------- 단어 관리 ----------

    @Transactional
    public VocabularyWordResponse addWord(CreateVocabularyWordRequest request) {
        if (request.setId() == null || !vocabularySetRepository.existsById(request.setId())) {
            throw new IllegalStateException("Part를 먼저 선택해주세요.");
        }
        if (request.word() == null || request.word().isBlank() || request.meaning() == null || request.meaning().isBlank()) {
            throw new IllegalStateException("단어와 뜻을 모두 입력해주세요.");
        }
        VocabularyWord saved = vocabularyWordRepository.save(
                VocabularyWord.create(request.setId(), request.word(), request.meaning(), request.example()));
        return toWordResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<VocabularyWordResponse> getWordsForSetAdmin(Long setId) {
        return vocabularyWordRepository.findBySetIdOrderByCreatedAtAsc(setId).stream()
                .map(this::toWordResponse)
                .toList();
    }

    @Transactional
    public void deleteWord(Long id) {
        vocabularyWordRepository.deleteById(id);
    }

    // ---------- 학생 ----------

    @Transactional(readOnly = true)
    public List<VocabularySetResponse> getSetsForStudent(String language) {
        return getSetsForLanguage(language);
    }

    @Transactional(readOnly = true)
    public List<FlashcardResponse> getFlashcards(Long setId) {
        return vocabularyWordRepository.findBySetIdOrderByCreatedAtAsc(setId).stream()
                .map(w -> new FlashcardResponse(w.getId(), w.getWord(), w.getMeaning(), w.getExample()))
                .toList();
    }

    @Transactional
    public void submitQuizResult(Long setId, String username, SubmitQuizResultRequest request) {
        if (!vocabularySetRepository.existsById(setId)) {
            throw new IllegalStateException("Part를 찾을 수 없어요.");
        }
        vocabularyQuizResultRepository.save(
                com.imkhun.imkhun.domain.VocabularyQuizResult.create(setId, username, request.score(), request.totalQuestions()));
    }

    @Transactional(readOnly = true)
    public QuizResultResponse getBestQuizResult(Long setId, String username) {
        return vocabularyQuizResultRepository.findTopBySetIdAndUsernameOrderByCompletedAtDesc(setId, username)
                .map(r -> new QuizResultResponse(r.getScore(), r.getTotalQuestions(), r.getCompletedAt().format(DATETIME_FORMAT)))
                .orElse(new QuizResultResponse(null, null, null));
    }

    private VocabularyWordResponse toWordResponse(VocabularyWord word) {
        return new VocabularyWordResponse(word.getId(), word.getSetId(), word.getWord(), word.getMeaning(), word.getExample());
    }
}