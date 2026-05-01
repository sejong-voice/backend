package com.example.sejong_voice.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.category.entity.Category;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.image.config.OciObjectStorageProperties;
import com.example.sejong_voice.image.dto.ImageUploadResponse;
import com.example.sejong_voice.image.dto.PostImageView;
import com.example.sejong_voice.image.entity.Image;
import com.example.sejong_voice.image.entity.PostImage;
import com.example.sejong_voice.image.exception.ImageErrorCode;
import com.example.sejong_voice.image.repository.ImageRepository;
import com.example.sejong_voice.image.repository.PostImageRepository;
import com.example.sejong_voice.image.repository.PostStatementImageRepository;
import com.example.sejong_voice.image.storage.OciImageStorage;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatement;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

  @Mock private ImageRepository imageRepository;
  @Mock private PostImageRepository postImageRepository;
  @Mock private PostStatementImageRepository postStatementImageRepository;
  @Mock private OciImageStorage ociImageStorage;

  private ImageService imageService;
  private Clock fixedClock;
  private Instant now;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    now = Instant.now(fixedClock);
    imageService =
        new ImageService(
            imageRepository,
            postImageRepository,
            postStatementImageRepository,
            ociImageStorage,
            new OciObjectStorageProperties("ap-seoul-1", "bucket", "namespace"),
            fixedClock);
  }

  @Test
  void upload_빈파일이면_EMPTY_FILE() {
    User user = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    MockMultipartFile file = new MockMultipartFile("image", new byte[0]);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> imageService.upload(user, file));

    assertSame(ImageErrorCode.EMPTY_FILE, exception.getErrorCode());
  }

  @Test
  void upload_지원하지않는_파일형식이면_UNSUPPORTED_FILE_TYPE() {
    User user = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    MockMultipartFile file =
        new MockMultipartFile("image", "a.txt", "text/plain", "abc".getBytes());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> imageService.upload(user, file));

    assertSame(ImageErrorCode.UNSUPPORTED_FILE_TYPE, exception.getErrorCode());
  }

  @Test
  void upload_성공하면_스토리지업로드후_이미지응답을_반환한다() throws IOException {
    User user = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    MockMultipartFile file = new MockMultipartFile("image", "a.png", "image/png", "abc".getBytes());
    when(ociImageStorage.upload(user.getId(), file, "image/png")).thenReturn("object/a.png");
    when(imageRepository.save(any(Image.class)))
        .thenAnswer(
            invocation -> {
              Image image = invocation.getArgument(0);
              ReflectionTestUtils.setField(
                  image, "id", UUID.fromString("00000000-0000-0000-0000-000000000111"));
              return image;
            });
    when(ociImageStorage.getPublicUrl("object/a.png"))
        .thenReturn("https://example.com/object/a.png");

    ImageUploadResponse result = imageService.upload(user, file);

    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000111"), result.imageId());
    assertEquals("https://example.com/object/a.png", result.imageUrl());
  }

  @Test
  void attachImagesToPost_정상이미지들을_순서대로_연결한다() {
    User user = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Post post = buildPost(UUID.randomUUID(), user);
    Image image1 = TestFixtures.image(UUID.randomUUID(), user, "object/1.png", now);
    Image image2 = TestFixtures.image(UUID.randomUUID(), user, "object/2.png", now);
    when(imageRepository.findAllByIdIn(List.of(image1.getId(), image2.getId())))
        .thenReturn(List.of(image1, image2));
    when(postImageRepository.existsByImageId(image1.getId())).thenReturn(false);
    when(postImageRepository.existsByImageId(image2.getId())).thenReturn(false);
    when(postStatementImageRepository.existsByImageId(image1.getId())).thenReturn(false);
    when(postStatementImageRepository.existsByImageId(image2.getId())).thenReturn(false);

    imageService.attachImagesToPost(user, post, List.of(image1.getId(), image2.getId()));

    ArgumentCaptor<PostImage> captor = ArgumentCaptor.forClass(PostImage.class);
    verify(postImageRepository, org.mockito.Mockito.times(2)).save(captor.capture());
    assertEquals(0, captor.getAllValues().get(0).getDisplayOrder());
    assertEquals(1, captor.getAllValues().get(1).getDisplayOrder());
  }

  @Test
  void attachImagesToPost_본인소유가_아니면_IMAGE_ACCESS_DENIED() {
    User user = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    User other = buildUser(UUID.randomUUID(), "전자정보공학과");
    Post post = buildPost(UUID.randomUUID(), user);
    Image image = TestFixtures.image(UUID.randomUUID(), other, "object/1.png", now);
    when(imageRepository.findAllByIdIn(List.of(image.getId()))).thenReturn(List.of(image));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> imageService.attachImagesToPost(user, post, List.of(image.getId())));

    assertSame(ImageErrorCode.IMAGE_ACCESS_DENIED, exception.getErrorCode());
  }

  @Test
  void getPostImages_공개URL로_변환한다() {
    User user = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Post post = buildPost(UUID.randomUUID(), user);
    Image image = TestFixtures.image(UUID.randomUUID(), user, "object/1.png", now);
    when(postImageRepository.findAllByPostIdOrderByDisplayOrderAsc(post.getId()))
        .thenReturn(List.of(TestFixtures.postImage(UUID.randomUUID(), post, image, 0, now)));
    when(ociImageStorage.getPublicUrl("object/1.png"))
        .thenReturn("https://example.com/object/1.png");

    List<PostImageView> result = imageService.getPostImages(post.getId());

    assertEquals(1, result.size());
    assertEquals(image.getId(), result.get(0).imageId());
  }

  @Test
  void cleanupExpiredTempImages_스토리지삭제후_이미지를_삭제처리한다() {
    User user = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Image image =
        TestFixtures.image(UUID.randomUUID(), user, "object/1.png", now.minusSeconds(60 * 60 * 25));
    when(imageRepository.findAllExpiredTempImagesBefore(now.minusSeconds(60 * 60 * 24)))
        .thenReturn(List.of(image));

    imageService.cleanupExpiredTempImages();

    verify(ociImageStorage).delete("object/1.png");
    assertEquals(now, image.getDeletedAt());
  }

  @Test
  void deleteAllByPostId_게시글과_입장문_이미지를_모두_삭제처리한다() {
    User user = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Post post = buildPost(UUID.randomUUID(), user);
    PostStatement statement =
        TestFixtures.postStatement(UUID.randomUUID(), post, user, 1, "입장문", now);
    Image postImage = TestFixtures.image(UUID.randomUUID(), user, "object/1.png", now);
    Image statementImage = TestFixtures.image(UUID.randomUUID(), user, "object/2.png", now);
    when(postImageRepository.findAllByPostIdOrderByDisplayOrderAsc(post.getId()))
        .thenReturn(List.of(TestFixtures.postImage(UUID.randomUUID(), post, postImage, 0, now)));
    when(postStatementImageRepository.findAllByPostStatementPostId(post.getId()))
        .thenReturn(
            List.of(
                TestFixtures.postStatementImage(
                    UUID.randomUUID(), statement, statementImage, 0, now)));

    imageService.deleteAllByPostId(post.getId());

    assertEquals(now, postImage.getDeletedAt());
    assertEquals(now, statementImage.getDeletedAt());
    verify(postImageRepository).deleteAllByPostId(post.getId());
    verify(postStatementImageRepository).deleteAllByPostStatementPostId(post.getId());
  }

  private User buildUser(UUID id, String affiliationName) {
    return TestFixtures.user(
        id,
        "20201234",
        "홍길동",
        TestFixtures.affiliation(UUID.randomUUID(), affiliationName),
        UserRole.STUDENT,
        now);
  }

  private Post buildPost(UUID id, User author) {
    Category category = TestFixtures.category(UUID.randomUUID(), "학사");
    Council council =
        TestFixtures.council(
            UUID.randomUUID(),
            TestFixtures.affiliation(UUID.randomUUID(), "총학생회"),
            author,
            10,
            new BigDecimal("0.5000"),
            now);
    return TestFixtures.post(
        id, author, council, category, PostStatus.VOTING, 10, new BigDecimal("0.5000"), now);
  }
}
