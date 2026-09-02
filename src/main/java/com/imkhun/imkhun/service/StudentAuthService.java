package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

// "KWZM Computer Training & Language Center" 전용 로그인 — 일반 사이트 로그인/관리자 로그인과 완전 별개
// 학생번호 + 아이디 + 비밀번호 + 이메일 4개가 모두 맞아야 하고, 그 신청 내역이 "승인완료" 상태여야 함
@Service
public class StudentAuthService {

    private static final String COOKIE_NAME = "studentToken";
    private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30; // 30일

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public StudentAuthService(UserRepository userRepository, ApplicationRepository applicationRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean login(HttpServletResponse response, String username, String password,
                         String email, String studentNumber) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();

        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
        boolean emailMatches = user.getEmail() != null && user.getEmail().equalsIgnoreCase(email);
        if (!passwordMatches || !emailMatches) return false;

        Optional<Application> appOpt = applicationRepository
                .findByUsernameAndStudentNumberAndStatus(username, studentNumber, "APPROVED");
        if (appOpt.isEmpty()) return false;

        String token = generateToken();
        user.updateStudentSessionToken(token);
        userRepository.save(user);
        setCookie(response, token, COOKIE_MAX_AGE_SECONDS);
        return true;
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        getLoggedInUser(request).ifPresent(user -> {
            user.updateStudentSessionToken(null);
            userRepository.save(user);
        });
        setCookie(response, "", 0);
    }

    public boolean isLoggedIn(HttpServletRequest request) {
        return getLoggedInUser(request).isPresent();
    }

    public Optional<User> getLoggedInUser(HttpServletRequest request) {
        String token = readCookie(request);
        if (token == null) return Optional.empty();
        return userRepository.findByStudentSessionToken(token);
    }

    // 로그인된 학생이 승인받은 강의(들) — 접근 가능한 자료 범위를 정할 때 씀
    public java.util.List<Application> getApprovedApplications(String username) {
        return applicationRepository.findByUsernameOrderByCreatedAtDesc(username).stream()
                .filter(a -> "APPROVED".equals(a.getStatus()))
                .toList();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void setCookie(HttpServletResponse response, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
    }

    private String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}