package com.example.sejong_voice.auth.jwt;


import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.auth.dto.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
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
    /*
    • parseClaims는 토큰을 서명 검증한 뒤 클레임 본문을 Claims 객체로 돌려줍니다. 이 Claims는
  Map<String, Object>처럼 동작하며, 이 코드 기준으로는 아래 값들이 들어 있습니다 (src/main/java/
  com/example/sejong_voice/global/security/JwtUtil.java:65):

  - sub: user.getStudentNo()로 넣은 subject
  - iss: jwtProperties.getIssuer()
  - role: user.getRole().name()로 넣은 커스텀 클레임
  - iat: 토큰 발급 시각
  - exp: 만료 시각
    즉, 비즈니스 로직에서 token의 상세 정보가 필요한 경우에 사용
      요약하면 “검증된 토큰의 payload 내용을 가져와 비즈니스 판단에 쓰는 용도”입니다.

     */


  // 헤더에서 토큰 추출 Bearer
  public String resolveToken(HttpServletRequest req) {
    String bearerToken = req.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

//    // 토큰을 바탕으로 스프링 시큐리티 인증 객체 생성
//    public Authentication getAuthentication(String token) {
//        Claims claims = parseClaims(token);
//        String studentNo = claims.getSubject();
//        String role = claims.get("role").toString();
//
//        //사용자 권한 정보를 담은 리슼트 생성
//        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_"+role));
//
//        // 유저 정봏를 담은 principal 객체 생성 (학번을 ID로 사용)
//        //var principal = new org.springframework.security.core.userdetails.User(studentNo, "", authorities);
//        //return new UsernamePasswordAuthenticationToken(principal, token, authorities);
//        //이 부분이 user로 만들고 있어서 UserRoleRequest랑은 파라미터가 맞지 않음
//
//    }

  public Authentication getAuthentication(String token) {
    Claims claims = parseClaims(token);

    String userId = claims.getSubject();
    String studentNo = claims.get("studentNo", String.class);
    String rawRole = claims.get("role", String.class);  // 예: "SUPER" 또는 "ROLE_SUPER"
    if (!StringUtils.hasText(rawRole)) {
      throw new IllegalArgumentException("Missing role claim");
    }

    // 1) enum 변환용 키 정규화
    String enumKey = rawRole.replace("ROLE_", "").toUpperCase(java.util.Locale.ROOT);//소문자 -> 대문자
    UserRole roleEnum = UserRole.valueOf(enumKey);// "SUPER" -> UserRole.SUPER

    // 2) authorities는 ROLE_ prefix로 고정
    List<SimpleGrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority("ROLE_" + roleEnum.name()));

    // 3) principal(UserRoleRequest)에 enum 넣기
    UserPrincipal principal = new UserPrincipal();
    principal.setUserId(userId);
    principal.setStudentNo(studentNo);
    principal.setRole(roleEnum);

    return new UsernamePasswordAuthenticationToken(principal, token, authorities);
  }
}
