package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "study_materials")
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "korean" / "japanese" / "thai" / "english"
    @Column(nullable = false)
    private String language;

    // "GRAMMAR" / "READING" / "WRITING" / "SPEAKING" / "OTHER"
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

    // 제목 밑에 들어가는 부가 설명 (선택)
    @Lob
    @Column(columnDefinition = "CLOB")
    private String description;

    // 이 자료를 볼 수 있는 학생번호들 — 비어있으면 그 언어를 듣는 전체 학생에게 보임,
    // 하나라도 들어있으면 그 학생(들)에게만 보임
    @ElementCollection
    @CollectionTable(name = "material_assigned_students", joinColumns = @JoinColumn(name = "material_id"))
    @Column(name = "student_number")
    private Set<String> assignedStudentNumbers = new HashSet<>();

    // 자료 하나에 파일(사진/문서)이 여러 개 딸릴 수 있음 — 갤러리처럼 묶어서 보여줌
    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<MaterialFile> files = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected StudyMaterial() {
        // JPA 기본 생성자
    }

    public static StudyMaterial create(String language, String category, String title, String description) {
        StudyMaterial material = new StudyMaterial();
        material.language = language;
        material.category = category;
        material.title = title;
        material.description = description;
        return material;
    }

    public void updateInfo(String language, String category, String title, String description) {
        this.language = language;
        this.category = category;
        this.title = title;
        this.description = description;
    }

    public void updateAssignedStudents(Set<String> studentNumbers) {
        this.assignedStudentNumbers.clear();
        if (studentNumbers != null) {
            this.assignedStudentNumbers.addAll(studentNumbers);
        }
    }

    // 새 파일을 올렸을 때, 기존 파일을 전부 지움 (그 다음 addFile로 새로 채움)
    public void clearFiles() {
        this.files.clear();
    }

    public void addFile(String fileName, String fileType, String fileData,
                        String linkUrl, String textContent, int sortOrder) {
        this.files.add(MaterialFile.create(this, fileName, fileType, fileData, linkUrl, textContent, sortOrder));
    }

    public Long getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getAssignedStudentNumbers() {
        return assignedStudentNumbers;
    }

    public List<MaterialFile> getFiles() {
        return files;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}