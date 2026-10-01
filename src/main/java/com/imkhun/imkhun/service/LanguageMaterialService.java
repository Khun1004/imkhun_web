package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.LanguageMaterial;
import com.imkhun.imkhun.dto.CreateLanguageMaterialRequest;
import com.imkhun.imkhun.dto.LanguageMaterialResponse;
import com.imkhun.imkhun.repository.LanguageMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// imkhun 공개 홈페이지 "강의 자료" 탭 — 로그인 없이 누구나 볼 수 있고, 관리자가 자유롭게 등록/수정/삭제/순서를 관리함
@Service
public class LanguageMaterialService {

    // 자료를 하나도 등록 안 했을 때, 화면이 비어 보이지 않도록 보여주는 예시 카드 — 원래 쓰시던 화면 그대로
    private static final String[] KOREAN_DEFAULT_TITLES = {"한국어 기초", "1급", "2급", "3급", "4급"};
    private static final String[] KOREAN_DEFAULT_DESCRIPTIONS = {
            "발음부터 기본 문장까지, 처음 시작하는 분들을 위한 자료",
            "기초 문법과 일상 회화 표현 정리 자료",
            "확장 문법과 주제별 어휘 학습 자료",
            "심화 문법과 실전 작문 연습 자료",
            "고급 표현과 시사 주제 독해 자료",
    };

    private static final String[] COMPUTER_DEFAULT_TITLES = {"기초", "Word", "Excel", "PowerPoint", "페이지메이커", "포토샵"};
    private static final String[] COMPUTER_DEFAULT_DESCRIPTIONS = {
            "컴퓨터 기본 개념과 자판·파일 관리 자료",
            "문서 작성과 서식 활용 자료",
            "표 계산과 함수, 기본 데이터 정리 자료",
            "발표 자료 디자인과 애니메이션 활용 자료",
            "편집 디자인 기초 자료",
            "이미지 보정과 합성 기초 자료",
    };

    private static final String DEFAULT_BADGE = "BEST";

    private final LanguageMaterialRepository languageMaterialRepository;
    private final FileStorageService fileStorageService;

    // 서버가 떠 있는 동안 예시 카드를 한 번만 확인/생성하도록 하는 표시 (재시작하면 다시 확인함)
    private volatile boolean defaultsChecked = false;

    public LanguageMaterialService(LanguageMaterialRepository languageMaterialRepository,
                                   FileStorageService fileStorageService) {
        this.languageMaterialRepository = languageMaterialRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public List<LanguageMaterialResponse> getAllMaterials() {
        return getOrCreateMaterials().stream().map(this::toResponse).toList();
    }

    @Transactional
    public LanguageMaterialResponse createMaterial(CreateLanguageMaterialRequest request) {
        validate(request);
        LanguageMaterial saved = languageMaterialRepository.save(
                LanguageMaterial.create(request.language(), request.sortOrder(), request.title(), request.description(),
                        request.badge(), request.imageUrl(), request.fileUrl(), request.fileName()));
        return toResponse(saved);
    }

    @Transactional
    public LanguageMaterialResponse updateMaterial(Long id, CreateLanguageMaterialRequest request) {
        validate(request);
        LanguageMaterial material = languageMaterialRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("자료를 찾을 수 없어요."));

        String oldImageUrl = material.getImageUrl();
        String oldFileUrl = material.getFileUrl();
        material.update(request.language(), request.sortOrder(), request.title(), request.description(),
                request.badge(), request.imageUrl(), request.fileUrl(), request.fileName());
        LanguageMaterial saved = languageMaterialRepository.save(material);

        // 이미지/첨부파일이 새 걸로 바뀌었거나 지워졌으면, 더 이상 안 쓰는 예전 파일을 디스크에서 정리함
        if (oldImageUrl != null && !oldImageUrl.equals(request.imageUrl())) {
            fileStorageService.delete(oldImageUrl);
        }
        if (oldFileUrl != null && !oldFileUrl.equals(request.fileUrl())) {
            fileStorageService.delete(oldFileUrl);
        }
        return toResponse(saved);
    }

    @Transactional
    public void deleteMaterial(Long id) {
        LanguageMaterial material = languageMaterialRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("자료를 찾을 수 없어요."));
        languageMaterialRepository.deleteById(id);
        if (material.getImageUrl() != null) {
            fileStorageService.delete(material.getImageUrl());
        }
        if (material.getFileUrl() != null) {
            fileStorageService.delete(material.getFileUrl());
        }
    }

    private void validate(CreateLanguageMaterialRequest request) {
        if (request.language() == null || request.language().isBlank()) {
            throw new IllegalStateException("언어를 선택해주세요.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalStateException("제목을 입력해주세요.");
        }
    }

    // 예시 카드(한국어 기초~4급, 컴퓨터 활용 기초)가 실제 DB에 없으면 만들어서 넣어둠 — 관리자 화면에도
    // 실제 자료처럼 보이고, 수정/삭제도 가능함. 이미 있는 자료가 있어도 상관없이, 그 언어에 그 제목의 자료가
    // 아직 없는 것만 하나씩 채워 넣음(이미 있으면 중복으로 또 만들지 않음).
    // + 예전 버전(언어 항목이 생기기 전)에 테스트로 등록됐던, 언어가 비어있는 낡은 자료는 자동으로 정리함
    private List<LanguageMaterial> getOrCreateMaterials() {
        if (!defaultsChecked) {
            removeLegacyMaterialsWithoutLanguage();
            removeOldSingleComputerExample();
            seedDefaults("korean", KOREAN_DEFAULT_TITLES, KOREAN_DEFAULT_DESCRIPTIONS);
            seedDefaults("computer", COMPUTER_DEFAULT_TITLES, COMPUTER_DEFAULT_DESCRIPTIONS);
            defaultsChecked = true;
        }
        return languageMaterialRepository.findAllByOrderByLanguageAscSortOrderAsc();
    }

    // 지난번에 실수로 만들어졌던 "컴퓨터 활용 기초" 예시 1개를 정리함(이제는 원래 화면처럼 6개짜리 세트로 바뀜)
    private void removeOldSingleComputerExample() {
        languageMaterialRepository.findAllByOrderByLanguageAscSortOrderAsc().stream()
                .filter(m -> "computer".equals(m.getLanguage()) && "컴퓨터 활용 기초".equals(m.getTitle()))
                .forEach(m -> languageMaterialRepository.deleteById(m.getId()));
    }

    // 언어 항목이 생기기 전, 테스트로 등록됐었던 "언어 없음" 자료를 지움. 관리자 화면에서 언어를 고르지 않고는
    // 더 이상 등록할 수 없으므로, 언어가 비어있는 자료는 전부 예전 테스트 데이터로 간주해도 안전함
    private void removeLegacyMaterialsWithoutLanguage() {
        List<LanguageMaterial> legacy = languageMaterialRepository.findAllByOrderByLanguageAscSortOrderAsc().stream()
                .filter(m -> m.getLanguage() == null || m.getLanguage().isBlank())
                .toList();
        for (LanguageMaterial material : legacy) {
            languageMaterialRepository.deleteById(material.getId());
            if (material.getImageUrl() != null) fileStorageService.delete(material.getImageUrl());
            if (material.getFileUrl() != null) fileStorageService.delete(material.getFileUrl());
        }
    }

    private void seedDefaults(String language, String[] titles, String[] descriptions) {
        List<LanguageMaterial> existing = languageMaterialRepository.findAllByOrderByLanguageAscSortOrderAsc();
        for (int i = 0; i < titles.length; i++) {
            String title = titles[i];
            boolean alreadyExists = existing.stream()
                    .anyMatch(m -> language.equals(m.getLanguage()) && title.equals(m.getTitle()));
            if (!alreadyExists) {
                languageMaterialRepository.save(LanguageMaterial.create(
                        language, i + 1, title, descriptions[i],
                        i == 0 ? DEFAULT_BADGE : null, null, null, null));
            }
        }
    }

    private LanguageMaterialResponse toResponse(LanguageMaterial material) {
        return new LanguageMaterialResponse(
                material.getId(), material.getLanguage(), material.getSortOrder(), material.getTitle(),
                material.getDescription(), material.getBadge(), material.getImageUrl(),
                material.getFileUrl(), material.getFileName());
    }
}