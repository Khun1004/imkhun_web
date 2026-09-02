package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "material_files")
public class MaterialFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private StudyMaterial material;

    @Column
    private String fileName;

    @Column
    private String fileType;

    // data:image/...;base64,... 형태로 그대로 저장
    @Lob
    @Column(columnDefinition = "CLOB")
    private String fileData;

    // 링크 등록일 때만 값이 있음
    @Column
    private String linkUrl;

    // 글 등록일 때만 값이 있음
    @Lob
    @Column(columnDefinition = "CLOB")
    private String textContent;

    // 갤러리에서 보여줄 순서
    @Column(nullable = false)
    private int sortOrder;

    protected MaterialFile() {
        // JPA 기본 생성자
    }

    public static MaterialFile create(StudyMaterial material, String fileName, String fileType,
                                      String fileData, String linkUrl, String textContent, int sortOrder) {
        MaterialFile file = new MaterialFile();
        file.material = material;
        file.fileName = fileName;
        file.fileType = fileType;
        file.fileData = fileData;
        file.linkUrl = linkUrl;
        file.textContent = textContent;
        file.sortOrder = sortOrder;
        return file;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFileData() {
        return fileData;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public String getTextContent() {
        return textContent;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}