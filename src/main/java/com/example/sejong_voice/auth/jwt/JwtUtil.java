package com.example.sejong_voice.auth.jwt;


import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import io.jsonwebtoken.security.Keys;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtUtil {

  private final JwtProperties jwtProperties;
  private SecretKey secretKey;

  @PostConstruct
  protected void init() {
    this.secretKey = Keys.hmacShaKeyFor(
        jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
  }

  //토큰 생성
  public String generateToken(String userId, String studentNo, String role, Long expiration) {
    return Jwts.builder()
        .setIssuer(jwtProperties.getIssuer())
        .setSubject(userId)
        .claim("studentNo", studentNo)
        .claim("role", role)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  //토큰 검증
  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);//이건 뭐하는거지
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  // 클레임 파싱
  // 유효한지만 보고 싶으면 validateToken을 쓰고,
  // 유효하다고 가정하고 토큰 안의 sub, role등 데이터를 꺼내려면 parseClaims
  public Claims parseClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  // 헤더에서 토큰 추출 Bearer
  public String resolveToken(HttpServletRequest req) {
    String bearerToken = req.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  public Authentication getAuthentication(String token) {
    // 1. 토큰 파싱
    Claims claims = parseClaims(token);

    // 2. userId 변환
    String subject = claims.getSubject();
    UUID userId;
    try {
      userId = UUID.fromString(subject);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(AuthErrorCode.INVALID_USER);
    }

    // 3. 기타 정보 추출
    String studentNo = claims.get("studentNo", String.class);
    String rawRole = claims.get("role", String.class);

    if (!StringUtils.hasText(rawRole)) {
      throw new BusinessException(AuthErrorCode.INVALID_USER);
    }

    // 4. Role Enum 변환
    String enumKey = rawRole.replace("ROLE_", "").toUpperCase(java.util.Locale.ROOT);
    UserRole roleEnum;
    try {
      roleEnum = UserRole.valueOf(enumKey);
    } catch (IllegalArgumentException e) {
      // DB나 Enum에 없는 이상한 권한이 들어왔을 경우
      throw new IllegalArgumentException("Invalid Role: " + enumKey);
    }

    // 5. Authorities 생성
    List<SimpleGrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleEnum.name()));

    // 6. UserPrincipal 객체 생성
    UserPrincipal principal = new UserPrincipal();
    principal.setUserId(userId);
    principal.setStudentNo(studentNo);
    principal.setRole(roleEnum);

    // 7. 인증 토큰 반환
    return new UsernamePasswordAuthenticationToken(principal, token, authorities);
  }
}
