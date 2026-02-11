package com.example.sejong_voice._init;

import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.council.policy.entity.CouncilPolicy;
import com.example.sejong_voice.council.policy.CouncilPolicyRepository;
import com.example.sejong_voice.council.repository.CouncilRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private final CouncilRepository councilRepository;
  private final CouncilPolicyRepository councilPolicyRepository;

  private static final int MIN_VOTE = 10;
  private static final double APPROVE_RATIO = 0.1;

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    if (councilRepository.count() > 0) {
      log.info("[Init] 학생회 기초 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
      return;
    }

    log.info("[Init] 학생회 기초 데이터 생성을 시작합니다...");

    // 1. 총학생회 생성
    Council totalCouncil = saveCouncil("총학생회", null);

    // 2. 단과대 및 학과 데이터 정의
    Map<String, List<String>> collegeMap = new LinkedHashMap<>();

    collegeMap.put("인문과학대학", Arrays.asList(
        "국어국문학과",
        "영어데이터융합",
        "국제일본학/일어일문학",
        "중국통상학",
        "역사학과",
        "교육학과"
    ));

    collegeMap.put("사회과학대학", Arrays.asList(
        "행정학과",
        "미디어커뮤니케이션학과",
        "법학과"
    ));

    collegeMap.put("경영경제대학", Arrays.asList(
        "경영학부",
        "경제학과"
    ));

    collegeMap.put("호텔관광대학", Arrays.asList(
        "호텔관광경영학",
        "외식경영학",
        "호텔외식관광프랜차이즈경영학과",
        "글로벌조리학과",
        "호텔외식비즈니스학과"
    ));

    collegeMap.put("자연과학대학", Arrays.asList(
        "수학통계학과",
        "물리천문학과",
        "화학과"
    ));

    collegeMap.put("생명과학대학", Arrays.asList(
        "식품생명공학",
        "바이오융합공학",
        "바이오산업자원공학",
        "스마트생명산업융합학과"
    ));

    collegeMap.put("인공지능융합대학", Arrays.asList(
        "AI융합전자공학과",
        "반도체시스템공학과",
        "컴퓨터공학과",
        "정보보호학과",
        "양자지능정보학과",
        "디자인이노베이션",
        "만화애니메이션텍",
        "지능IoT학과",
        "사이버국방학과",
        "국방AI로봇융합공학과",
        "인공지능데이터사이언스학과",
        "AI로봇학과",
        "지능정보융합학과",
        "콘텐츠소프트웨어학과/소프트웨어학과"
    ));

    collegeMap.put("공과대학", Arrays.asList(
        "건축공학과",
        "건축학과",
        "건설환경공학과",
        "환경융합공학과",
        "지구자원시스템공학과",
        "기계공학과",
        "우주항공공학",
        "지능형드론융합",
        "항공시스템공학",
        "나노신소재공학과",
        "양자원자력공학과",
        "국방AI융합시스템공학과/국방시스템공학과"
    ));

    collegeMap.put("예체능대학", Arrays.asList(
        "회화과",
        "패션디자인학과",
        "음악과",
        "체육학과",
        "무용과",
        "영화예술학과"
    ));

    // 3. 데이터 삽입 로직
    for (String collegeName : collegeMap.keySet()) {
      // 단과대 생성 (부모: 총학생회)
      Council college = saveCouncil(collegeName, totalCouncil);

      // 해당 단과대 소속 학과들 생성 (부모: 단과대)
      List<String> departmentList = collegeMap.get(collegeName);
      for (String departmentName : departmentList) {
        saveCouncil(departmentName, college);
      }
    }

    log.info("[Init] 학생회 기초 데이터 생성 완료: 총학생회 + 단과대 + 학과");
  }

  // Council 및 Policy 동시 생성 헬퍼 메서드
  private Council saveCouncil(String name, Council parent) {
    // 1. Council 저장
    Council council = Council.builder()
        .name(name)
        .parent(parent)
        .build();
    Council savedCouncil = councilRepository.save(council);

    // 2. Policy 저장
    CouncilPolicy policy = CouncilPolicy.builder()
        .council(savedCouncil)
        .minVote(MIN_VOTE) // 10
        .approveRatioThreshold(BigDecimal.valueOf(APPROVE_RATIO)) // 0.1
        .build();
    councilPolicyRepository.save(policy);

    return savedCouncil;
  }
}