package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.dto.ChangePasswordRequest;
import com.imkhun.imkhun.dto.MyPageResponse;
import com.imkhun.imkhun.dto.UpdateNicknameRequest;
import com.imkhun.imkhun.dto.UpdatePhoneRequest;
import com.imkhun.imkhun.dto.UpdatePhotoRequest;
import com.imkhun.imkhun.repository.UserRepository;
import com.imkhun.imkhun.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/mypage")
public class MyPageController {

    private final UserService userService;
    private final UserRepository userRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public MyPageController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    private boolean notLoggedIn(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal());
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (notLoggedIn(authentication)) return ResponseEntity.status(401).build();

        return userRepository.findByUsername(authentication.getName())
                .map(u -> ResponseEntity.ok(new MyPageResponse(
                        u.getId(), u.getEmail(), u.getUsername(), u.getNickname(),
                        u.getPhone(), u.getProfileImage(), u.getCreatedAt().format(DATE_FORMAT)
                )))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @PostMapping("/phone")
    public ResponseEntity<?> updatePhone(Authentication authentication,
                                         @RequestBody UpdatePhoneRequest request) {
        if (notLoggedIn(authentication)) return ResponseEntity.status(401).build();

        try {
            userService.changePhone(authentication.getName(), request.phone());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/nickname")
    public ResponseEntity<?> updateNickname(Authentication authentication,
                                            @RequestBody UpdateNicknameRequest request) {
        if (notLoggedIn(authentication)) return ResponseEntity.status(401).build();

        try {
            userService.changeNickname(authentication.getName(), request.nickname());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(Authentication authentication,
                                            @RequestBody ChangePasswordRequest request) {
        if (notLoggedIn(authentication)) return ResponseEntity.status(401).build();

        try {
            userService.changePassword(authentication.getName(), request.currentPassword(), request.newPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/photo")
    public ResponseEntity<?> updatePhoto(Authentication authentication,
                                         @RequestBody UpdatePhotoRequest request) {
        if (notLoggedIn(authentication)) return ResponseEntity.status(401).build();

        try {
            userService.changeProfileImage(authentication.getName(), request.imageBase64());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}