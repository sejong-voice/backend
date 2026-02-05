package com.example.sejong_voice.repository;


import com.example.sejong_voice.auth.User;
import com.example.sejong_voice.auth.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class UserRepositoryTest {
    @Autowired
    UserRepository userRepository;

    // 저장되는지 테스트
    @Test
    void saveAndFindUser(){
        User user = User.builder()
                .studentNo("22109029")
                .name("박박박")
                .department("컴퓨터공학과")
                .build();

        userRepository.save(user);

        assertThat(userRepository.findByStudentNo("22109029"))
                .isPresent()
                .get()
                .extracting(User::getName)
                .isEqualTo(user.getName());
    }


}
