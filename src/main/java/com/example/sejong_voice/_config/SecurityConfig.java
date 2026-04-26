package com.example.sejong_voice._config;


import com.example.sejong_voice._config.filter.JwtAuthenticationFilter;
import com.example.sejong_voice._config.filter.UserStatusFilter;
import com.example.sejong_voice.auth.jwt.JwtUtil;
import com.example.sejong_voice.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtUtil jwtUtil;//토큰 검증용
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private List<String> allowedOrigins;


  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // 1. REST API이므로 필요 없는 기능 끄기
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // 2. 경로별 권한 설정 (위에서부터 우선순위 적용)
        // api 경로만 수정하면 되긴함
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh")
            .permitAll()//refresh는 토큰이 만료되어도 접근가능하게 해용해두어ㅑ하는데

            .requestMatchers(HttpMethod.GET, "/api/v1/posts/*/votes/statistics")
            .hasAnyRole("ADMIN", "SUPER")
            .requestMatchers(HttpMethod.GET, "/api/v1/posts/*/votes/agree-voters")
            .hasAnyRole("ADMIN", "SUPER")

            // GET 요청에 대해 누구나 접근 가능하도록 허용
            .requestMatchers(HttpMethod.GET, "/api/v1/posts/**")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/comments/**")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/councils/**")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/categories/**")
            .permitAll()

            // Auth
            .requestMatchers("/api/v1/auth/profile").authenticated()
            .requestMatchers("/api/v1/auth/logout").authenticated()
            // 신고 생성은 로그인 사용자 허용
            .requestMatchers("/api/v1/reports/posts/*").authenticated()
            .requestMatchers("/api/v1/reports/comments/*").authenticated()

            // SUPER만 허용 (정확히 찍기)
            .requestMatchers("/api/v1/admin/user/*/department").hasRole("SUPER")
            .requestMatchers("/api/v1/admin/user/*/status").hasRole("SUPER")
            .requestMatchers("/api/v1/admin/user/*/role").hasRole("SUPER")
            .requestMatchers("/api/v1/admin/user/*/restore").hasRole("SUPER")

            .requestMatchers("/api/v1/admin/moderations/posts/*").hasRole("SUPER")
            .requestMatchers("/api/v1/admin/moderations/comments/*").hasRole("SUPER")
            .requestMatchers("/api/v1/admin/reports/users/*/comments/count").hasRole("SUPER")
            .requestMatchers("/api/v1/admin/reports/posts/*/count").hasRole("SUPER")

            .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER")

            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

            // 나머지는 로그인한 유저라면 모두 가능
            .anyRequest().authenticated()
        );
    // 3. JWT 필터 적용 (UsernamePasswordAuthenticationFilter 이전에 실행)
    http.addFilterBefore(new JwtAuthenticationFilter(jwtUtil),
        UsernamePasswordAuthenticationFilter.class);

    http.addFilterAfter(new UserStatusFilter(userRepository, objectMapper, clock),
        JwtAuthenticationFilter.class);

    return http.build();
  }

  //비밀번호 암호화를 위한 빈 (BCrypt 사용)
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With")
    );
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
