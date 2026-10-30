package interview.guide.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtService 测试")
class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret("test-secret-for-jwt");
    properties.setExpireMinutes(60);
    jwtService = new JwtService(properties);
  }

  @Test
  @DisplayName("签发后能校验成功并还原 AuthUser")
  void issueAndVerify() {
    String token = jwtService.issueToken(1L, "candidate", Role.CANDIDATE);

    assertThat(jwtService.verify(token)).contains(new AuthUser(1L, "candidate", Role.CANDIDATE));
  }

  @Test
  @DisplayName("错误密钥校验失败")
  void wrongKeyFails() {
    JwtProperties otherProps = new JwtProperties();
    otherProps.setSecret("another-secret");
    otherProps.setExpireMinutes(60);
    JwtService other = new JwtService(otherProps);

    String token = jwtService.issueToken(1L, "candidate", Role.CANDIDATE);

    assertThat(other.verify(token)).isEmpty();
  }

  @Test
  @DisplayName("过期 token 校验失败")
  void expiredTokenFails() {
    String token = jwtService.issueToken(1L, "candidate", Role.CANDIDATE, -1);

    assertThat(jwtService.verify(token)).isEmpty();
  }

  @Test
  @DisplayName("格式错误的 token 校验失败")
  void malformedTokenFails() {
    assertThat(jwtService.verify("not-a-jwt")).isEmpty();
    assertThat(jwtService.verify(null)).isEmpty();
    assertThat(jwtService.verify("")).isEmpty();
  }
}