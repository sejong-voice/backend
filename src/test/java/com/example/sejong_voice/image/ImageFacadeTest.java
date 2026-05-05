package com.example.sejong_voice.image;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.image.dto.ImageUploadResponse;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageFacadeTest {

  @Mock private ImageService imageService;
  @Mock private UserService userService;

  private ImageFacade imageFacade;

  @BeforeEach
  void setUp() {
    imageFacade = new ImageFacade(imageService, userService);
  }

  @Test
  void upload_유저를_조회한_뒤_업로드를_위임한다() {
    UUID userId = UUID.randomUUID();
    User user =
        TestFixtures.user(
            userId,
            "20201234",
            "홍길동",
            TestFixtures.affiliation(UUID.randomUUID(), "컴퓨터공학과"),
            UserRole.STUDENT,
            Instant.parse("2026-05-01T00:00:00Z"));
    MockMultipartFile file = new MockMultipartFile("image", "a.png", "image/png", "img".getBytes());
    ImageUploadResponse response =
        ImageUploadResponse.of(UUID.randomUUID(), "https://example.com/a.png");
    when(userService.getById(userId)).thenReturn(user);
    when(imageService.upload(user, file)).thenReturn(response);

    assertSame(response, imageFacade.upload(userId, file));
  }

  @Test
  void cleanupExpiredTempImages_서비스에_위임한다() {
    imageFacade.cleanupExpiredTempImages();

    verify(imageService).cleanupExpiredTempImages();
  }
}
