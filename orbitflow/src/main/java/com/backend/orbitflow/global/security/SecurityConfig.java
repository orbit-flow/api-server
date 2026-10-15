package com.backend.orbitflow.global.security;

import java.util.List;

import com.backend.orbitflow.domain.auth.advice.OAuth2FailureHandler;
import com.backend.orbitflow.domain.auth.advice.OAuth2SuccessHandler;
import com.backend.orbitflow.domain.auth.advice.OAuth2UserLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration 
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor 
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2UserLoader oAuth2UserLoader;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final SecurityErrorResponder securityErrorResponder;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                        "/swagger-resources/**", "/webjars/**", "/errors/**",
                        "/api/auth/**", "/oauth2/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                // 오류 응답(/error)은 ErrorResponseController가 JSON으로 반환
                .requestMatchers("/error").permitAll()
                // WebSocket 핸드셰이크는 허용, 인증은 STOMP CONNECT 프레임에서 수행 (StompAuthInterceptor)
                .requestMatchers("/ws/**").permitAll()

                // actuator는 별도 관리 포트(management.server.port)에서만 열리며 접근 제어는 네트워크(NGINX IP 제한)에서 수행
                .requestMatchers("/actuator/**").permitAll()

                // 플랫폼 운영 관리 기능은 관리자만
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
        );

        http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo ->
                        userInfo.userService(oAuth2UserLoader)
                ).successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
        );

        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint(securityErrorResponder)
                .accessDeniedHandler(securityErrorResponder)
        );

        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:8000"
        ));

        configuration.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "Accept",
                "Origin"
        ));

        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        configuration.setAllowCredentials(true);

        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Disposition"
        ));

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 전체 API
        source.registerCorsConfiguration("/api/**", configuration);

        // WebSocket
        source.registerCorsConfiguration("/ws/**", configuration);

        return source;
    }
}
