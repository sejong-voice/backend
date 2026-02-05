package com.example.sejong_voice.auth;


import com.example.sejong_voice.auth.dto.*;
import com.example.sejong_voice.auth.jwt.JwtProperties;
import com.example.sejong_voice.auth.jwt.JwtUtil;
import com.example.sejong_voice.global.error.ErrorCode;
import com.example.sejong_voice.global.error.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SchoolAuthClient schoolAuthClient;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    // 1. 로그인 (DB 조회 후 없으면 sso 인증)
    @Transactional
    public LoginResponse login(LoginRequest request){
        //1. DB 조회 먼저
        User user = userRepository.findByStudentNo(request.studentNo())
                .orElseGet(()-> {
                    // 2. DB에 유저가 없다면 파이썬 서버로 SSO 인증 요청
                    // 사용자가 입력한 학번과 비번을 그대로 ssoRequest에 담아 보냅니다.
                    SsoResponse ssoResponse = schoolAuthClient.getStudentProfile(
                            new SsoRequest(request.studentNo(), request.password())
                    );


                    // 3. sso 성공 시 받은 정보로 신규 가입 (비번은 db용으로 암호화 저장)
                    return userRepository.save(User.builder()
                                    .studentNo(ssoResponse.user().studentNo())
                                    .name(ssoResponse.user().name())
                                    .department(ssoResponse.user().department())
                                    .password(passwordEncoder.encode(request.password())) // 암호화 추가!
                                    .build());
                });

        // 4. DB에 있던 유저라면 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())){
            throw new InvalidPasswordException();//커스텀 에러
        }


        // 5. 계정 상태 체크 (정지 여부)
        if (user.getStatus() == UserStatus.SUSPENDED){
            throw new UserSuspendedException();//커스텀 에러
        }

        return generateLoginResponse(user);//
    }

    // 2. 로그아웃
    @Transactional
    public void logout(String  studentNo){
        User targetUser = userRepository.findByStudentNo(studentNo)
                .orElseThrow(UserNotFoundException::new);
        refreshTokenRepository.deleteByUser(targetUser);//deleteByUser로 해도 되긴함
    }

    // 3. 토큰 재발급 (RefreshToken 유효, AccessToken 재발급)
    @Transactional
    public RefreshResponse accessRefresh(String refreshToken){ //studentId로 유저 조회, 해당 user_id로 refreshToken 조회후 요청
        //DB 존재 여부 확인
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.INVALID_TOKEN));//커스텀 에러
        // 토큰 유효성 검사
        if(storedToken.isExpired()){
            refreshTokenRepository.deleteByToken(refreshToken);
            throw new InvalidTokenException(ErrorCode.TOKEN_EXPIRED);//커스텀 에러
        }

        // accessToken 재발급
        User user = storedToken.getUser();
        String newAccessToken = jwtUtil.generateToken(user,jwtProperties.getAccessTokenExpiration());//3600L

        return new RefreshResponse(
                user.getStudentNo(),
                newAccessToken,
                jwtProperties.getAccessTokenExpiration()//3600L
        );
    }

    // 4.유저 정보 조회_--------
    public UserSearchResponse getUserProfile(String studentNo){
        User user = userRepository.findByStudentNo(studentNo)
                .orElseThrow(UserNotFoundException::new);//커스텀 에러
        return UserSearchResponse.from(user);
    }


    // 5. 유저 정보 수정 (Super 계정 전용 - 학과 수정)
    @Transactional
    public void updateDepartment(UserRoleRequest superRole, String targetStudentNo, String newDepartment){
        if(superRole.getRole()!=UserRole.SUPER){//super계정인 경우에
            throw new NotSuperAccountException();
        }
        User targetUser = userRepository.findByStudentNo(targetStudentNo)
                .orElseThrow(UserNotFoundException::new);//커스텀 에러
        targetUser.updateDepartment(newDepartment);
    }

    // 6. 상태 변경 (Super 계정 전용 - 제재 및 토큰 파기)
    @Transactional
    public void updateStatus(UserRoleRequest superRole, String targetStudentNo, UserStatus newStatus, LocalDateTime newDuration) {
        if(superRole.getRole()!=UserRole.SUPER){//super계정인 경우에
            throw new NotSuperAccountException();
        }
        User targetUser = userRepository.findByStudentNo(targetStudentNo)
                .orElseThrow(UserNotFoundException::new);//커스텀 에러
        targetUser.updateStatus(newStatus, newDuration);

        // 제재suspended이면 refreshToken 즉시파기
        if(newStatus.equals(UserStatus.SUSPENDED)){
            refreshTokenRepository.deleteByUser(targetUser);
        }
    }

    // 7. 권한 수정 (Super 계정 전용 - Role 변경)
    @Transactional
    public void updateRole(UserRoleRequest superRole, String targetStudentNo, UserRole newRole){
        // 1. 권한이 SUPER가 아니면 즉시 예외 발생
        if (superRole.getRole() != UserRole.SUPER) {
            throw new NotSuperAccountException(); //
        }

        // 2. 권한이 확인된 경우에만 이후 로직 실행
        User targetUser = userRepository.findByStudentNo(targetStudentNo)
                .orElseThrow(UserNotFoundException::new);
        targetUser.updateRole(newRole);
    }



    //Login반환값 만들어주는 로직 (refreshToken)
    private LoginResponse generateLoginResponse(User user) {
        // 1. 토큰 생성 (AccessToken: 1시간, RefreshToken: 14일 가정)
        String accessToken = jwtUtil.generateToken(user, jwtProperties.getAccessTokenExpiration());//초단위라고 하는데 전에 밀리세컨 단위로 적어야 한다는 둥....
        String refreshToken = jwtUtil.generateToken(user, jwtProperties.getRefreshTokenExpiration());//로그인시 토큰 유효시간 문제 발생하면 뭐.. 여기 문제인거지

        // 2. 토큰 만들어서 그냥 넣어주면 됨 refreshToken은 재로그인시 업데이트
        // 1. 기존 토큰이 있는지 확인하고 삭제 (있든 없든 일단 비우기)
        refreshTokenRepository.deleteByUser(user);

        // 2. 새 토큰 정보를 그냥 "새로" 넣어주기
        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiryDate(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))//1209600000L
                .build();

        refreshTokenRepository.flush();
        refreshTokenRepository.save(newToken);

        // 3. 제공해주신 LoginResponseDto 규격에 맞춰 반환
        return new LoginResponse(
                user.getStudentNo(), // studentNo
                accessToken,        // accessToken
                jwtProperties.getAccessTokenExpiration()/1000// expiresIn (1시간을 초 단위로 표기) 3600L
        );
    }

}
