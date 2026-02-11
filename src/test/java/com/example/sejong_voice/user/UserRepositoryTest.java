package com.example.sejong_voice.user;

import com.example.sejong_voice._config.QueryDslConfig;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
class UserRepositoryTest {

  @Autowired
  UserRepository userRepository;

  @Test
  @DisplayName("회원가입 시 기본값(Role, Status, Date)이 자동으로 설정된다")
  void saveAndFindUser() {
    // given
    User user = User.builder()
        .studentNo("22109029")
        .name("박박박")
        .department("컴퓨터공학과")
        .password("123456")
        .build();

    // when
    User savedUser = userRepository.save(user);

    // then
    assertThat(savedUser.getId()).isNotNull();
    assertThat(savedUser.getName()).isEqualTo("박박박");
    assertThat(savedUser.getRole()).isEqualTo(UserRole.STUDENT);
    assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(savedUser.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("학과별 사용자 조회가 정상 작동한다")
  void findByDepartment() {
    // given
    User user1 = User.builder().studentNo("111").name("A").department("컴공").password("pw").build();
    User user2 = User.builder().studentNo("222").name("B").department("컴공").password("pw").build();
    User user3 = User.builder().studentNo("333").name("C").department("무용").password("pw").build();

    userRepository.saveAll(List.of(user1, user2, user3));

    // when
    List<User> comUsers = userRepository.findByDepartment("컴공");

    // then
    assertThat(comUsers).hasSize(2);
  }
}