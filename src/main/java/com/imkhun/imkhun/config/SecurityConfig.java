package com.imkhun.imkhun.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                          UserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    // 아이디+비밀번호 로그인을 직접 처리할 때(AuthController) 사용
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider::authenticate;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // H2 콘솔 화면 자체가 프레임(frame)으로 만들어져 있어서,
                // 기본 보안 정책(X-Frame-Options: DENY)을 풀어주지 않으면 화면이 안 뜸
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // 구글 로그인은 리다이렉트를 여러 번 오가면서 세션에 상태를 잠깐 저장해야 해서
                // STATELESS 대신 필요할 때만 세션을 만드는 IF_REQUIRED로 변경
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .logout(logout -> logout
                        // 프론트에서 <a href="/logout">으로 그냥 이동만 해도 되게 GET 허용
                        // (CSRF를 꺼둔 작은 프로젝트라 괜찮음, 나중에 실서비스에선 POST로 바꾸는 게 안전)
                        .logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/logout"))
                        .logoutSuccessUrl("/")
                );
        return http.build();
    }
}