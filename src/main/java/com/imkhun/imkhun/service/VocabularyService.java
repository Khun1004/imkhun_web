package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.VocabularyProgress;
import com.imkhun.imkhun.domain.VocabularyWord;
import com.imkhun.imkhun.dto.CreateVocabularyWordRequest;
import com.imkhun.imkhun.dto.FlashcardResponse;
import com.imkhun.imkhun.dto.VocabularyStatsResponse;
import com.imkhun.imkhun.dto.VocabularyWordResponse;
import com.imkhun.imkhun.repository.VocabularyProgressRepository;
import com.imkhun.imkhun.repository.VocabularyWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VocabularyService {

    private final VocabularyWordRepository vocabularyWordRepository;
    private final VocabularyProgressRepository vocabularyProgressRepository;

    public VocabularyService(VocabularyWordRepository vocabularyWordRepository,
                             VocabularyProgressRepository vocabularyProgressRepository) {
        this.vocabularyWordRepository = vocabularyWordRepository;
        this.vocabularyProgressRepository = vocabularyProgressRepository;
    }

    // ---------- 관리자 ----------

    @Transactional
    public VocabularyWordResponse addWord(CreateVocabularyWordRequest request) {
        if (request.language() == null || request.language().isBlank()
                || request.word() == null || request.word().isBlank()
                || request.meaning() == null || request.meaning().isBlank()) {
            throw new IllegalStateException("언어, 단어, 뜻을 모두 입력해주세요.");
        }
        VocabularyWord saved = vocabularyWordRepository.save(
                VocabularyWord.create(request.language(), request.word(), request.meaning(), request.example()));
        return toWordResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<VocabularyWordResponse> getWordsForAdmin(String language) {
        return vocabularyWordRepository.findByLanguageOrderByCreatedAtDesc(language).stream()
                .map(this::toWordResponse)
                .toList();
    }

    @Transactional
    public void deleteWord(Long id) {
        vocabularyWordRepository.deleteById(id);
    }

    // ---------- 학생 ----------

    @Transactional(readOnly = true)
    public List<FlashcardResponse> getFlashcards(String username, String language) {
        List<VocabularyWord> words = vocabularyWordRepository.findByLanguageOrderByCreatedAtDesc(language);
        Map<Long, Boolean> learnedByWordId = vocabularyProgressRepository.findByUsername(username).stream()
                .collect(Collectors.toMap(VocabularyProgress::getWordId, VocabularyProgress::isLearned));

        return words.stream()
                .map(w -> new FlashcardResponse(w.getId(), w.getWord(), w.getMeaning(), w.getExample(),
                        learnedByWordId.getOrDefault(w.getId(), false)))
                .toList();
    }

    @Transactional
    public void markLearned(String username, Long wordId, boolean learned) {
        VocabularyProgress progress = vocabularyProgressRepository.findByUsernameAndWordId(username, wordId)
                .orElseGet(() -> VocabularyProgress.create(username, wordId, learned));
        progress.update(learned);
        vocabularyProgressRepository.save(progress);
    }

    @Transactional(readOnly = true)
    public VocabularyStatsResponse getStats(String username, String language) {
        List<VocabularyWord> words = vocabularyWordRepository.findByLanguageOrderByCreatedAtDesc(language);
        List<Long> wordIds = words.stream().map(VocabularyWord::getId).toList();

        List<VocabularyProgress> myProgress = vocabularyProgressRepository.findByUsername(username).stream()
                .filter(p -> wordIds.contains(p.getWordId()))
                .toList();

        long learnedTotal = myProgress.stream().filter(VocabularyProgress::isLearned).count();
        LocalDate today = LocalDate.now();
        long learnedToday = myProgress.stream()
                .filter(VocabularyProgress::isLearned)
                .filter(p -> p.getUpdatedAt().toLocalDate().equals(today))
                .count();

        return new VocabularyStatsResponse(words.size(), learnedTotal, learnedToday);
    }

    private VocabularyWordResponse toWordResponse(VocabularyWord word) {
        return new VocabularyWordResponse(word.getId(), word.getLanguage(), word.getWord(), word.getMeaning(), word.getExample());
    }
}