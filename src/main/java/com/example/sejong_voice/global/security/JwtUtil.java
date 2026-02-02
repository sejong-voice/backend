package com.example.sejong_voice.global.security;


import com.example.sejong_voice.auth.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import io.jsonwebtoken.security.Keys;


/*
@RequiredArgsConstructor
final이 붙은 필드나 @NonNull이 붙은 필드만을 파라미터로 받는 생성자를 자동으로 생성해주는 어노테이션입니다.
작성하신 JwtProperties 클래스처럼 설정 값을 불변(immutable)으로 유지하거나, 스프링에서 **의존성 주입(DI)**을 받을 때 매우 유용하게 사용됩니다.
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    protected void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    //토큰 생성
    public String generateToken(User user,Long expiration){
        return Jwts.builder()
                .setIssuer(jwtProperties.getIssuer())
                .setSubject(user.getStudentNo())
                .claim("role",user.getRole().name())//enum값인 role을 문자열로 사용하기위해 name으로 변환
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    //토큰 검증
    public boolean validateToken(String token){
        try{
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);//이건 뭐하는거지
            /*
              - setSigningKey(secretKey)로 서명 검증에 쓸 비밀키(HS256)를 지정합니다.
              - parseClaimsJws(token)가 토큰을 디코딩하며 서명 일치 여부, 포맷, 만료(exp), 발행시각(iat) 등
                을 확인합니다.
              - 여기서는 반환값을 쓰지 않고 예외 발생 여부만 봅니다. 예외가 없으면 true, 서명 불일치·만료·형
                식 오류 등이 나면 catch로 떨어져 false를 리턴하는 구조입니다.
            */
            return true;
        } catch (Exception e){
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
}
