package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.MaterialView;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.MaterialViewerResponse;
import com.imkhun.imkhun.repository.MaterialViewRepository;
import com.imkhun.imkhun.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

// 학생이 "온라인 영상" 자료를 열어봤는지 기록하고, 관리자한테 누가 봤는지 보여줌
@Service
public class MaterialViewService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final MaterialViewRepository materialViewRepository;
    private final UserRepository userRepository;

    public MaterialViewService(MaterialViewRepository materialViewRepository, UserRepository userRepository) {
        this.materialViewRepository = materialViewRepository;
        this.userRepository = userRepository;
    }

    // 같은 학생이 같은 영상을 여러 번 열어봐도 기록은 하나만 남고, 마지막으로 본 시간만 갱신돼요
    @Transactional
    public void recordView(Long materialId, String username) {
        MaterialView view = materialViewRepository.findByMaterialIdAndUsername(materialId, username)
                .orElseGet(() -> MaterialView.create(materialId, username));
        view.touch();
        materialViewRepository.save(view);
    }

    @Transactional(readOnly = true)
    public List<MaterialViewerResponse> getViewers(Long materialId) {
        return materialViewRepository.findByMaterialIdOrderByViewedAtDesc(materialId).stream()
                .map(v -> {
                    User user = userRepository.findByUsername(v.getUsername()).orElse(null);
                    String nickname = user != null ? user.getNickname() : v.getUsername();
                    return new MaterialViewerResponse(v.getUsername(), nickname, v.getViewedAt().format(DATE_FORMAT));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public long getViewCount(Long materialId) {
        return materialViewRepository.countByMaterialId(materialId);
    }

    // 자료 목록 화면에서 여러 영상의 시청 인원수를 한 번에 보여줄 때 씀
    @Transactional(readOnly = true)
    public java.util.Map<Long, Long> getViewCounts(List<Long> materialIds) {
        java.util.Map<Long, Long> counts = new java.util.HashMap<>();
        for (Long id : materialIds) {
            counts.put(id, materialViewRepository.countByMaterialId(id));
        }
        return counts;
    }
}