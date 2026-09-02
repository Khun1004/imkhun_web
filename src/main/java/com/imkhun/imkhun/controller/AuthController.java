package com.imkhun.imkhun.controller;

import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.dto.*;
import com.imkhun.imkhun.repository.UserRepository;
import com.imkhun.imkhun.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(UserService userService, UserRepository userRepository,
                          AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
    }

    // STEP 1 → 2: 구글 인증 직후, 구글이 알려준 이메일/닉네임을 프론트에 전달
    @GetMapping("/google-profile")
    public ResponseEntity<?> googleProfile(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String email = principal.getAttribute("email");
        String nickname = principal.getAttribute("name");
        return ResponseEntity.ok(new GoogleProfileResponse(email, nickname));
    }

    // 아이디 중복확인
    @GetMapping("/check-username")
    public ResponseEntity<UsernameAvailabilityResponse> checkUsername(@RequestParam String username) {
        return ResponseEntity.ok(new UsernameAvailabilityResponse(userService.isUsernameAvailable(username)));
    }

    // STEP 2: 구글 인증 + 아이디/비밀번호로 최종 회원가입
    @PostMapping("/complete-signup")
    public ResponseEntity<?> completeSignup(@AuthenticationPrincipal OAuth2User principal,
                                            @RequestBody CompleteSignupRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body("구글 인증 정보가 없어요. 처음부터 다시 시도해주세요.");
        }
        String email = principal.getAttribute("email");
        String nickname = principal.getAttribute("name");

        try {
            userService.completeSignup(email, nickname, request.username(), request.password());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // STEP 3: 아이디 + 비밀번호 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authRequest =
                    new UsernamePasswordAuthenticationToken(request.username(), request.password());
            Authentication authentication = authenticationManager.authenticate(authRequest);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            User user = userRepository.findByUsername(request.username()).orElseThrow();
            return ResponseEntity.ok(new SignupResponse(user.getId(), user.getEmail(), user.getNickname()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    // 현재 로그인한 사용자 정보 (헤더 프로필 표시용)
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).build();
        }

        Optional<User> user = userRepository.findByUsername(authentication.getName());
        return user
                .map(u -> ResponseEntity.ok(new MeResponse(u.getId(), u.getEmail(), u.getNickname(), u.getProfileImage())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    // ---------- 아이디 / 비밀번호 찾기 (구글 재인증 기반) ----------

    // 구글 재인증 후, 그 이메일로 가입된 계정이 있는지 확인
    @GetMapping("/recovery-info")
    public ResponseEntity<?> recoveryInfo(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String email = principal.getAttribute("email");

        return userService.findByEmail(email)
                .map(u -> ResponseEntity.ok(new RecoveryInfoResponse(true, u.getUsername())))
                .orElseGet(() -> ResponseEntity.ok(new RecoveryInfoResponse(false, null)));
    }

    // 구글 재인증된 상태에서 새 비밀번호로 재설정
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@AuthenticationPrincipal OAuth2User principal,
                                           @RequestBody ResetPasswordRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body("구글 인증 정보가 없어요. 처음부터 다시 시도해주세요.");
        }
        String email = principal.getAttribute("email");

        try {
            userService.resetPasswordByEmail(email, request.newPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}