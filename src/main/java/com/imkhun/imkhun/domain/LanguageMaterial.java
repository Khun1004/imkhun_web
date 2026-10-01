package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// imkhun 공개 홈페이지 "강의 자료" 탭에 나오는 자료 카드 하나 — 로그인 안 해도 누구나 볼 수 있고,
// 언어별로 묶여서 카드들이 오른쪽에서 왼쪽으로 한 칸씩 천천히 자동으로 넘어가는 캐러셀로 보여줌.
// 관리자가 자유롭게 등록/수정/삭제/순서 변경 가능
@Entity
@Table(name = "language_materials")
public class LanguageMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카드가 묶이는 언어 그룹 — "korean"/"japanese"/"thai"/"english"/"computer"/"other"
    @Column(nullable = false)
    private String language;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    // 카드 오른쪽 위에 붙는 작은 리본 글자 (예: "BEST") — 없으면 리본이 안 보임
    @Column(name = "badge")
    private String badge;

    // 슬라이드에 보여줄 이미지 — /api/admin/upload로 올린 "/uploads/..." 경로. 안 넣으면 장식용 원 그래픽만 보여줌
    @Column(name = "image_url")
    private String imageUrl;

    // "자료 보기"를 누르면 새 창으로 열리는 첨부 파일 (PDF, 워드, 한글 파일 등) — /api/admin/upload로 올린 "/uploads/..." 경로
    @Column(name = "file_url")
    private String fileUrl;

    // 화면에 "OO.pdf" 식으로 보여줄 원래 파일 이름 (fileUrl은 서버에 저장될 때 임의의 이름으로 바뀌어서, 원래 이름은 따로 저장해둠)
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected LanguageMaterial() {
        // JPA 기본 생성자
    }

    public static LanguageMaterial create(String language, int sortOrder, String title, String description,
                                          String badge, String imageUrl, String fileUrl, String fileName) {
        LanguageMaterial material = new LanguageMaterial();
        material.language = language;
        material.sortOrder = sortOrder;
        material.title = title;
        material.description = description;
        material.badge = badge;
        material.imageUrl = imageUrl;
        material.fileUrl = fileUrl;
        material.fileName = fileName;
        return material;
    }

    public void update(String language, int sortOrder, String title, String description, String badge,
                       String imageUrl, String fileUrl, String fileName) {
        this.language = language;
        this.sortOrder = sortOrder;
        this.title = title;
        this.description = description;
        this.badge = badge;
        this.imageUrl = imageUrl;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
    }

    public Long getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBadge() {
        return badge;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}