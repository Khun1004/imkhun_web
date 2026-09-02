package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Admin;
import com.imkhun.imkhun.repository.AdminRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
public class AdminAuthService {

    // 브라우저 쿠키 이름 — 이 쿠키 값을 DB(admins.session_token)와 대조해서 로그인 여부를 판단함.
    // 스프링 시큐리티의 세션과 완전히 별개라, 서버(devtools)가 재시작돼도 로그인이 안 풀림.
    private static final String COOKIE_NAME = "adminToken";
    private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30; // 30일

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminAuthService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 최초 관리자 계정 등록 — admins 테이블이 비어있을 때 딱 한 번만 허용
    public void signup(String username, String rawPassword, String email, String phone) {
        if (adminRepository.count() > 0) {
            throw new IllegalStateException("이미 관리자 계정이 있어요.");
        }
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.length() < 8
                || email == null || email.isBlank() || phone == null || phone.isBlank()) {
            throw new IllegalStateException("모든 항목을 올바르게 입력해주세요 (비밀번호는 8자 이상).");
        }

        Admin admin = Admin.create(username, passwordEncoder.encode(rawPassword), email, phone);
        adminRepository.save(admin);
    }

    // 이메일, 전화번호, 아이디, 비밀번호 4개가 전부 맞아야 성공 — 성공하면 토큰을 만들어 쿠키+DB에 저장
    public boolean login(HttpServletResponse response, String username, String password, String email, String phone) {
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isEmpty()) {
            return false;
        }
        Admin admin = adminOpt.get();

        boolean passwordMatches = passwordEncoder.matches(password, admin.getPassword());
        boolean emailMatches = admin.getEmail().equalsIgnoreCase(email);
        boolean phoneMatches = admin.getPhone().replaceAll("\\s|-", "")
                .equals(phone == null ? "" : phone.replaceAll("\\s|-", ""));

        if (!passwordMatches || !emailMatches || !phoneMatches) {
            return false;
        }

        String token = generateToken();
        admin.updateSessionToken(token);
        adminRepository.save(admin);

        setCookie(response, token, COOKIE_MAX_AGE_SECONDS);
        return true;
    }

    public boolean isLoggedIn(HttpServletRequest request) {
        return getLoggedInAdmin(request).isPresent();
    }

    // 지금 쿠키가 어떤 관리자의 것인지 (아이디)
    public String getLoggedInUsername(HttpServletRequest request) {
        return getLoggedInAdmin(request).map(Admin::getUsername).orElse(null);
    }

    private Optional<Admin> getLoggedInAdmin(HttpServletRequest request) {
        String token = readCookie(request);
        if (token == null) return Optional.empty();
        return adminRepository.findBySessionToken(token);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        getLoggedInAdmin(request).ifPresent(admin -> {
            admin.updateSessionToken(null);
            adminRepository.save(admin);
        });
        setCookie(response, "", 0);
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