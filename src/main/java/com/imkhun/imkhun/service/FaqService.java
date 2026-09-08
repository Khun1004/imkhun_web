package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Faq;
import com.imkhun.imkhun.dto.CreateFaqRequest;
import com.imkhun.imkhun.dto.FaqResponse;
import com.imkhun.imkhun.repository.FaqRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FaqService {

    private final FaqRepository faqRepository;

    public FaqService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Transactional(readOnly = true)
    public List<FaqResponse> getAllFaqs() {
        return faqRepository.findAllByOrderByCreatedAtAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FaqResponse createFaq(CreateFaqRequest request) {
        validate(request);
        Faq saved = faqRepository.save(Faq.create(request.question(), request.answer()));
        return toResponse(saved);
    }

    @Transactional
    public FaqResponse updateFaq(Long id, CreateFaqRequest request) {
        validate(request);
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("FAQ를 찾을 수 없어요."));
        faq.update(request.question(), request.answer());
        return toResponse(faqRepository.save(faq));
    }

    @Transactional
    public void deleteFaq(Long id) {
        if (!faqRepository.existsById(id)) {
            throw new IllegalStateException("FAQ를 찾을 수 없어요.");
        }
        faqRepository.deleteById(id);
    }

    private void validate(CreateFaqRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            throw new IllegalStateException("질문을 입력해주세요.");
        }
        if (request.answer() == null || request.answer().isBlank()) {
            throw new IllegalStateException("답변을 입력해주세요.");
        }
    }

    private FaqResponse toResponse(Faq faq) {
        return new FaqResponse(faq.getId(), faq.getQuestion(), faq.getAnswer());
    }
}