package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.StudyMaterial;
import com.imkhun.imkhun.dto.CreateMaterialRequest;
import com.imkhun.imkhun.dto.MaterialFileRequest;
import com.imkhun.imkhun.dto.MaterialFileResponse;
import com.imkhun.imkhun.dto.MaterialResponse;
import com.imkhun.imkhun.repository.StudyMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
public class StudyMaterialService {

    private final StudyMaterialRepository studyMaterialRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final Set<String> VALID_LANGUAGES = Set.of("korean", "japanese", "thai", "english", "other");
    private static final Set<String> VALID_CATEGORIES = Set.of("GRAMMAR", "READING", "WRITING", "SPEAKING", "OTHER");

    public StudyMaterialService(StudyMaterialRepository studyMaterialRepository) {
        this.studyMaterialRepository = studyMaterialRepository;
    }

    @Transactional
    public MaterialResponse createMaterial(CreateMaterialRequest request) {
        validate(request);

        StudyMaterial material = StudyMaterial.create(request.language(), request.category(), request.title(), request.description());
        addFiles(material, request.files());
        StudyMaterial saved = studyMaterialRepository.save(material);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> getMaterials(String language, String category) {
        return studyMaterialRepository.findByLanguageAndCategoryOrderByCreatedAtDesc(language, category)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 학생 홈 화면 "최근 등록된 자료" — 승인받은 언어들 중 최근 N개
    @Transactional(readOnly = true)
    public List<MaterialResponse> getRecentMaterials(Set<String> languages, int limit) {
        if (languages == null || languages.isEmpty()) return List.of();
        return studyMaterialRepository.findByLanguageInOrderByCreatedAtDesc(languages)
                .stream()
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MaterialResponse updateMaterial(Long id, CreateMaterialRequest request) {
        validate(request);

        StudyMaterial material = studyMaterialRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("자료를 찾을 수 없어요."));
        material.updateInfo(request.language(), request.category(), request.title(), request.description());

        // 새 파일을 골랐을 때만 기존 파일을 지우고 교체 (안 골랐으면 기존 파일 유지)
        if (request.files() != null && !request.files().isEmpty()) {
            material.clearFiles();
            addFiles(material, request.files());
        }

        StudyMaterial saved = studyMaterialRepository.save(material);
        return toResponse(saved);
    }

    public void deleteMaterial(Long id) {
        if (!studyMaterialRepository.existsById(id)) {
            throw new IllegalStateException("자료를 찾을 수 없어요.");
        }
        studyMaterialRepository.deleteById(id);
    }

    private void addFiles(StudyMaterial material, List<MaterialFileRequest> files) {
        if (files == null) return;
        int order = 0;
        for (MaterialFileRequest file : files) {
            material.addFile(file.fileName(), file.fileType(), file.fileData(),
                    file.linkUrl(), file.textContent(), order++);
        }
    }

    private void validate(CreateMaterialRequest request) {
        if (!VALID_LANGUAGES.contains(request.language())) {
            throw new IllegalStateException("올바르지 않은 언어예요.");
        }
        if (!VALID_CATEGORIES.contains(request.category())) {
            throw new IllegalStateException("올바르지 않은 항목이에요.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalStateException("제목을 입력해주세요.");
        }
    }

    private MaterialResponse toResponse(StudyMaterial material) {
        List<MaterialFileResponse> fileResponses = material.getFiles().stream()
                .map(f -> new MaterialFileResponse(f.getFileName(), f.getFileType(), f.getFileData(),
                        f.getLinkUrl(), f.getTextContent()))
                .toList();

        return new MaterialResponse(
                material.getId(), material.getLanguage(), material.getCategory(), material.getTitle(),
                material.getDescription(), fileResponses, material.getCreatedAt().format(DATE_FORMAT)
        );
    }
}