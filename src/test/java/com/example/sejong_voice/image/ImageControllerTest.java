package com.example.sejong_voice.image;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sejong_voice._common.security.UserPrincipalArgumentResolver;
import com.example.sejong_voice.exception.GlobalExceptionHandler;
import com.example.sejong_voice.image.dto.ImageUploadResponse;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.entity.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ImageControllerTest {

  @Mock private ImageFacade imageFacade;

  private MockMvc mockMvc;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    Clock fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ImageController(imageFacade))
            .setControllerAdvice(new GlobalExceptionHandler(fixedClock))
            .setCustomArgumentResolvers(new UserPrincipalArgumentResolver())
            .setMessageConverters(new MappingJackson2HttpMessageConverter())
            .build();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void upload_로그인사용자로_업로드한다() throws Exception {
    authenticate();
    UUID imageId = UUID.randomUUID();
    MockMultipartFile file = new MockMultipartFile("image", "a.png", "image/png", "img".getBytes());
    when(imageFacade.upload(userId, file))
        .thenReturn(ImageUploadResponse.of(imageId, "https://example.com/a.png"));

    mockMvc
        .perform(multipart("/api/v1/images").file(file))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.imageId").value(imageId.toString()))
        .andExpect(jsonPath("$.imageUrl").value("https://example.com/a.png"));

    verify(imageFacade).upload(userId, file);
  }

  private void authenticate() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                TestFixtures.principal(userId, UserRole.STUDENT), "token", List.of()));
  }
}
