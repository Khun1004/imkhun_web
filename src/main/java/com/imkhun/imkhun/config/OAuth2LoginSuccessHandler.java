package com.imkhun.imkhun.config;

import com.imkhun.imkhun.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 구글 인증은 여기서는 "본인 확인"용일 뿐, DB에 회원을 직접 저장하지는 않음.
    // 이미 가입된 이메일이면 "계정 찾기" 흐름으로, 처음 보는 이메일이면 "회원가입" 흐름으로 보냄.
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        boolean alreadyRegistered = userRepository.existsByEmail(email);
        String redirectUrl = alreadyRegistered ? "/?googleRecovery=true" : "/?googleVerified=true";

        response.sendRedirect(redirectUrl);
    }
}