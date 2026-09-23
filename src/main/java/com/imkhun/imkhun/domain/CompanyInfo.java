package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// "회사소개"(COMPANY)와 "사업소개"(BUSINESS), 이 2개를 같은 구조로 저장함.
// imkhun 공개 사이트, KWZM Center 둘 다 이 데이터를 그대로 가져다 씀
@Entity
@Table(name = "company_info")
public class CompanyInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "COMPANY" | "BUSINESS"
    @Column(nullable = false, unique = true)
    private String type;

    @Column(nullable = false)
    private String eyebrow;

    @Column(nullable = false)
    private String title;

    // 문단은 빈 줄(\n\n)로 구분해서 하나의 텍스트로 저장함
    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected CompanyInfo() {
        // JPA 기본 생성자
    }

    public static CompanyInfo create(String type, String eyebrow, String title, String content) {
        CompanyInfo info = new CompanyInfo();
        info.type = type;
        info.eyebrow = eyebrow;
        info.title = title;
        info.content = content;
        return info;
    }

    public void update(String eyebrow, String title, String content) {
        this.eyebrow = eyebrow;
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getEyebrow() {
        return eyebrow;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}