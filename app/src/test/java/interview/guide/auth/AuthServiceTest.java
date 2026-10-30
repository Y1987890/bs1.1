package interview.guide.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import interview.guide.common.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@DisplayName("AuthService 测试")
class AuthServiceTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final JwtService jwtService = mock(JwtService.class);
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);

  private UserEntity candidateUser;

  @BeforeEach
  void setUp() {
    candidateUser = new UserEntity();
    candidateUser.setId(1L);
    candidateUser.setUsername("candidate");
    candidateUser.setPasswordHash(passwordEncoder.encode("candidate123"));
    candidateUser.setRole(Role.CANDIDATE);
    candidateUser.setEnabled(true);
  }

  @Test
  @DisplayName("密码正确返回 token")
  void correctPasswordReturnsToken() {
    when(userRepository.findByUsername("candidate")).thenReturn(Optional.of(candidateUser));
    when(jwtService.issueToken(1L, "candidate", Role.CANDIDATE)).thenReturn("fake-token");

    AuthToken token = authService.login("candidate", "candidate123");

    assertThat(token.token()).isEqualTo("fake-token");
    assertThat(token.user()).isEqualTo(new AuthUser(1L, "candidate", Role.CANDIDATE));
  }

  @Test
  @DisplayName("密码错误抛出 BusinessException")
  void wrongPasswordThrows() {
    when(userRepository.findByUsername("candidate")).thenReturn(Optional.of(candidateUser));

    assertThatThrownBy(() -> authService.login("candidate", "wrong-password"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("用户名或密码错误");
  }

  @Test
  @DisplayName("角色入口校验：入口角色与账号角色一致时正常登录")
  void expectedRoleMatchAllowsLogin() {
    when(userRepository.findByUsername("candidate")).thenReturn(Optional.of(candidateUser));
    when(jwtService.issueToken(1L, "candidate", Role.CANDIDATE)).thenReturn("fake-token");

    AuthToken token = authService.login("candidate", "candidate123", "CANDIDATE");

    assertThat(token.token()).isEqualTo("fake-token");
    assertThat(token.user().role()).isEqualTo(Role.CANDIDATE);
  }

  @Test
  @DisplayName("角色入口校验：求职者账号从管理员入口登录被拒绝且不签发 token")
  void expectedRoleMismatchRejected() {
    when(userRepository.findByUsername("candidate")).thenReturn(Optional.of(candidateUser));

    assertThatThrownBy(() -> authService.login("candidate", "candidate123", "ADMIN"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("该账号是「求职者」账号，请前往对应的登录入口");
    verify(jwtService, org.mockito.Mockito.never()).issueToken(any(), any(), any());
  }

  @Test
  @DisplayName("角色入口校验：密码错误时不泄露角色信息，仍提示用户名或密码错误")
  void wrongPasswordHidesRoleBeforeEntranceCheck() {
    when(userRepository.findByUsername("candidate")).thenReturn(Optional.of(candidateUser));

    assertThatThrownBy(() -> authService.login("candidate", "wrong-password", "ADMIN"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("用户名或密码错误");
  }

  @Test
  @DisplayName("角色入口校验：无效的入口角色返回登录入口无效")
  void invalidExpectedRoleRejected() {
    when(userRepository.findByUsername("candidate")).thenReturn(Optional.of(candidateUser));

    assertThatThrownBy(() -> authService.login("candidate", "candidate123", "SUPERUSER"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("登录入口无效");
  }

  @Test
  @DisplayName("disabled 用户拒绝登录")
  void disabledUserRejected() {
    candidateUser.setEnabled(false);
    when(userRepository.findByUsername("candidate")).thenReturn(Optional.of(candidateUser));

    assertThatThrownBy(() -> authService.login("candidate", "candidate123"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("注册接口强制创建 CANDIDATE 角色")
  void registerForcesCandidateRole() {
    when(userRepository.existsByUsername("newbie")).thenReturn(false);
    when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
      UserEntity u = inv.getArgument(0);
      u.setId(10L);
      return u;
    });

    AuthUser created = authService.register("newbie", "password123", "新同学");

    assertThat(created.role()).isEqualTo(Role.CANDIDATE);
    assertThat(created.username()).isEqualTo("newbie");
    verify(userRepository).save(argThat(u -> u.getRole() == Role.CANDIDATE));
  }

  @Test
  @DisplayName("密码长度不在 6-64 位之间抛出异常")
  void passwordLengthValidation() {
    assertThatThrownBy(() -> authService.register("newbie", "12345", "新同学"))
        .isInstanceOf(BusinessException.class);
  }
}