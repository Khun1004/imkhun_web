package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.MaterialFile;
import com.imkhun.imkhun.domain.StudyMaterial;
import com.imkhun.imkhun.dto.CreateMaterialRequest;
import com.imkhun.imkhun.dto.MaterialFileRequest;
import com.imkhun.imkhun.dto.MaterialFileResponse;
import com.imkhun.imkhun.dto.MaterialResponse;
import com.imkhun.imkhun.repository.StudyMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class StudyMaterialService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final FileStorageService fileStorageService;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final Set<String> VALID_LANGUAGES = Set.of("korean", "japanese", "thai", "english", "other", "computer", "video");
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "GRAMMAR", "READING", "WRITING", "SPEAKING", "OTHER",
            "BASIC", "WORD", "EXCEL", "POWERPOINT", "PAGEMAKER", "PHOTOSHOP",
            "VIDEO", "TRIAL"
    );

    private static final Set<String> VALID_SCOPES = Set.of("PERSONAL", "KWZM", "VIDEO", "TRIAL");
    private static final Set<String> VALID_LEVELS = Set.of("BEGINNER", "LEVEL1", "LEVEL2", "LEVEL3", "LEVEL4");

    public StudyMaterialService(StudyMaterialRepository studyMaterialRepository, FileStorageService fileStorageService) {
        this.studyMaterialRepository = studyMaterialRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public MaterialResponse createMaterial(CreateMaterialRequest request, String scope) {
        validate(request);

        StudyMaterial material = StudyMaterial.create(request.language(), request.category(), request.title(), request.description(), scope, request.level());
        material.updateAssignedStudents(toStudentNumberSet(request.assignedStudentNumbers()));
        addFiles(material, request.files());
        StudyMaterial saved = studyMaterialRepository.save(material);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> getMaterials(String language, String category, String scope) {
        return studyMaterialRepository.findByLanguageAndCategoryAndScopeOrderByCreatedAtDesc(language, category, scope)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 한국어 KWZM 자료 — 관리자가 등급을 하나 골라서 볼 때 (level이 null이면 등급 구분 없이 예전처럼 전체)
    @Transactional(readOnly = true)
    public List<MaterialResponse> getMaterialsByLevel(String language, String category, String scope, String level) {
        List<StudyMaterial> results = (level == null)
                ? studyMaterialRepository.findByLanguageAndCategoryAndScopeOrderByCreatedAtDesc(language, category, scope)
                : studyMaterialRepository.findByLanguageAndCategoryAndScopeAndLevelOrderByCreatedAtDesc(language, category, scope, level);
        return results.stream().map(this::toResponse).toList();
    }

    // 한국어 KWZM 자료 — 학생이 속한 등급(들) 안에 있는 것만 (levels가 비어있으면 등급 구분 없는 언어라 전체를 보여줌)
    @Transactional(readOnly = true)
    public List<MaterialResponse> getMaterialsForLevels(String language, String category, String scope, Set<String> levels) {
        if (levels == null || levels.isEmpty()) {
            return getMaterials(language, category, scope);
        }
        return studyMaterialRepository.findByLanguageAndCategoryAndScopeAndLevelInOrderByCreatedAtDesc(language, category, scope, levels)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // "내 수강 정보" 카드를 눌렀을 때 — 항목(category) 구분 없이 그 언어의 자료를 전부 보여줌
    @Transactional(readOnly = true)
    public List<MaterialResponse> getAllMaterialsForLanguage(String language, String scope) {
        return studyMaterialRepository.findByLanguageAndScopeOrderByCreatedAtDesc(language, scope)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // "내 수강 정보" 카드 — 한국어일 때는 학생이 속한 등급(들) 안에 있는 것만 (levels가 비어있으면 전체)
    @Transactional(readOnly = true)
    public List<MaterialResponse> getAllMaterialsForLanguageAndLevels(String language, String scope, Set<String> levels) {
        if (levels == null || levels.isEmpty()) {
            return getAllMaterialsForLanguage(language, scope);
        }
        return studyMaterialRepository.findByLanguageAndScopeAndLevelInOrderByCreatedAtDesc(language, scope, levels)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 검색 — 제목/설명에 검색어가 들어간 자료 (같은 scope 안에서만)
    @Transactional(readOnly = true)
    public List<MaterialResponse> searchMaterials(String keyword, String scope) {
        return studyMaterialRepository.searchByScope(scope, keyword)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 학생 홈 화면 "최근 등록된 자료" — 승인받은 언어들 중 최근 N개 (항상 KWZM 자료만)
    @Transactional(readOnly = true)
    public List<MaterialResponse> getRecentMaterials(Set<String> languages, int limit) {
        if (languages == null || languages.isEmpty()) return List.of();
        return studyMaterialRepository.findByLanguageInAndScopeOrderByCreatedAtDesc(languages, "KWZM")
                .stream()
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MaterialResponse updateMaterial(Long id, CreateMaterialRequest request, String scope) {
        validate(request);

        StudyMaterial material = studyMaterialRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("자료를 찾을 수 없어요."));
        if (!material.getScope().equals(scope)) {
            throw new IllegalStateException("자료를 찾을 수 없어요.");
        }
        material.updateInfo(request.language(), request.category(), request.title(), request.description(), request.level());
        material.updateAssignedStudents(toStudentNumberSet(request.assignedStudentNumbers()));

        // 새 파일을 골랐을 때만 기존 파일을 지우고 교체 (안 골랐으면 기존 파일 유지)
        if (request.files() != null && !request.files().isEmpty()) {
            List<String> oldFileUrls = material.getFiles().stream().map(MaterialFile::getFileData).toList();
            material.clearFiles();
            addFiles(material, request.files());
            StudyMaterial saved = studyMaterialRepository.save(material);
            oldFileUrls.forEach(fileStorageService::delete); // 저장 성공한 뒤에 예전 파일들을 디스크에서 지움
            return toResponse(saved);
        }

        StudyMaterial saved = studyMaterialRepository.save(material);
        return toResponse(saved);
    }

    @Transactional
    public void deleteMaterial(Long id, String scope) {
        StudyMaterial material = studyMaterialRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("자료를 찾을 수 없어요."));
        if (!material.getScope().equals(scope)) {
            throw new IllegalStateException("자료를 찾을 수 없어요.");
        }
        List<String> fileUrls = material.getFiles().stream().map(MaterialFile::getFileData).toList();
        studyMaterialRepository.deleteById(id);
        fileUrls.forEach(fileStorageService::delete); // DB에서 지운 뒤에 디스크 파일도 정리
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
        if (request.level() != null && !VALID_LEVELS.contains(request.level())) {
            throw new IllegalStateException("올바르지 않은 등급이에요.");
        }
    }

    private Set<String> toStudentNumberSet(List<String> studentNumbers) {
        if (studentNumbers == null) return new HashSet<>();
        return new HashSet<>(studentNumbers);
    }

    private MaterialResponse toResponse(StudyMaterial material) {
        List<MaterialFileResponse> fileResponses = material.getFiles().stream()
                .map(f -> new MaterialFileResponse(f.getFileName(), f.getFileType(), f.getFileData(),
                        f.getLinkUrl(), f.getTextContent()))
                .toList();

        return new MaterialResponse(
                material.getId(), material.getLanguage(), material.getCategory(), material.getTitle(),
                material.getDescription(), fileResponses, material.getCreatedAt().format(DATE_FORMAT),
                material.getAssignedStudentNumbers().stream().toList(), material.getScope(), material.getLevel()
        );
    }
}