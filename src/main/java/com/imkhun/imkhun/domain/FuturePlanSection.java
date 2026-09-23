package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// "미래 계획" 페이지 전체의 인트로 문단 + 맨 아래 CTA 문구. 딱 한 줄(싱글톤)만 있음
@Entity
@Table(name = "future_plan_section")
public class FuturePlanSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "intro_text", nullable = false, columnDefinition = "LONGTEXT")
    private String introText;

    @Column(name = "cta_title", nullable = false)
    private String ctaTitle;

    @Lob
    @Column(name = "cta_text", nullable = false, columnDefinition = "LONGTEXT")
    private String ctaText;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected FuturePlanSection() {
        // JPA 기본 생성자
    }

    public static FuturePlanSection create(String introText, String ctaTitle, String ctaText) {
        FuturePlanSection section = new FuturePlanSection();
        section.introText = introText;
        section.ctaTitle = ctaTitle;
        section.ctaText = ctaText;
        return section;
    }

    public void update(String introText, String ctaTitle, String ctaText) {
        this.introText = introText;
        this.ctaTitle = ctaTitle;
        this.ctaText = ctaText;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getIntroText() {
        return introText;
    }

    public String getCtaTitle() {
        return ctaTitle;
    }

    public String getCtaText() {
        return ctaText;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}