package com.example.sejong_voice._config;


import com.example.sejong_voice.auth.jwt.JwtAuthenticationFilter;
import com.example.sejong_voice.auth.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;//토큰 검증용

  // “엔티티냐 DTO냐”가 핵심이 아니라, @AuthenticationPrincipal이 주입하는 대상이 ‘SecurityContext의 principal’인데,
  // 지금 그 principal이 UserRoleRequest(DTO) 형태가 아니기 때문에 문제가 난다가 핵심
  // 1순위: JwtAuthenticationFilter에서 UsernamePasswordAuthenticationToken 생성하는 부분 (principal을 UserRoleRequest로 바꾸기)
  // 2순위: JwtUtil에 getRole() (필요하면 getStudentNo()) 추가/수정

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. REST API이므로 필요 없는 기능 끄기
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. 경로별 권한 설정 (위에서부터 우선순위 적용)
                // api 경로만 수정하면 되긴함
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()//refresh는 토큰이 만료되어도 접근가능하게 해용해두어ㅑ하는데
                        // Auth
                        .requestMatchers("/api/v1/auth/profile").authenticated()
                        .requestMatchers("/api/v1/auth/logout").authenticated()

                        // SUPER만 허용 (정확히 찍기)
                        .requestMatchers("/api/v1/admin/user/*/department").hasRole("SUPER")
                        .requestMatchers("/api/v1/admin/user/*/status").hasRole("SUPER")
                        .requestMatchers("/api/v1/admin/user/*/role").hasRole("SUPER")

                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER")

                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // 나머지는 로그인한 유저라면 모두 가능
                        .anyRequest().authenticated()
                );
        // 3. JWT 필터 적용 (UsernamePasswordAuthenticationFilter 이전에 실행)
        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }

    //비밀번호 암호화를 위한 빈 (BCrypt 사용)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
