package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    // 구글 인증(email, nickname) + 사용자가 정한 아이디/비밀번호로 최종 가입 완료
    public User completeSignup(String email, String nickname, String username, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("이미 가입된 구글 계정입니다.");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }

        User user = User.create(
                email,
                nickname,
                username,
                passwordEncoder.encode(rawPassword),
                "google"
        );
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void changeNickname(String username, String newNickname) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
        user.changeNickname(newNickname);
        userRepository.save(user);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalStateException("현재 비밀번호가 올바르지 않습니다.");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void changeProfileImage(String username, String imageBase64) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
        user.changeProfileImage(imageBase64);
        userRepository.save(user);
    }

    public void changePhone(String username, String phone) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
        user.changePhone(phone);
        userRepository.save(user);
    }

    // 구글 재인증(email)으로 비밀번호를 재설정 — 본인 확인은 OAuth2 세션으로 이미 끝난 상태
    public void resetPasswordByEmail(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("가입 내역을 찾을 수 없습니다."));
        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 아이디를 가려서 보여줌 (예: khunsw123 -> kh*******)
    public String maskUsername(String username) {
        if (username.length() <= 2) return username;
        return username.substring(0, 2) + "*".repeat(username.length() - 2);
    }
}