package com.imkhun.imkhun.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

// 이미지/파일을 DB(base64)가 아니라 서버 디스크에 저장하고, 짧은 경로(URL)만 돌려주는 서비스
@Service
public class FileStorageService {

    // application.properties에서 app.upload.dir로 바꿀 수 있음 (기본값: 프로젝트 폴더 밑의 uploads 폴더)
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("파일이 비어있어요.");
        }

        String extension = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(storedName);
        try (var in = file.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // 이 URL은 WebConfig에서 등록해둔 "/uploads/**" 경로로 그대로 서빙됨
        return "/uploads/" + storedName;
    }

    // 자료를 삭제하거나 새 파일로 교체할 때, 더 이상 안 쓰는 파일을 디스크에서 지워줌
    // url이 "/uploads/..."로 시작하는 실제 저장된 파일이 아니면(예: 예전 base64 데이터, 링크, null) 그냥 무시함
    public void delete(String url) {
        if (url == null || !url.startsWith("/uploads/")) return;

        String storedName = url.substring("/uploads/".length());
        if (storedName.isBlank() || storedName.contains("..") || storedName.contains("/") || storedName.contains("\\")) {
            return; // 이상한 경로는 안전하게 무시
        }

        try {
            Path targetPath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(storedName);
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            // 파일 하나 못 지운다고 전체 작업(자료 삭제/수정)을 실패시킬 필요는 없어서 조용히 넘어감
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).replaceAll("[^a-zA-Z0-9]", "");
    }
}