package interview.guide.auth;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthToken login(String username, String password) {
    return login(username, password, null);
  }

  /**
   * 登录（支持角色入口校验）。
   *
   * <p>三角色独立登录入口（/login/candidate 等）传入 expectedRole；
   * 凭据校验通过后，若账号实际角色与入口期望不符，返回明确的业务错误，
   * 不签发 token。expectedRole 为空表示通用入口，不限制角色。</p>
   */
  public AuthToken login(String username, String password, String expectedRole) {
    UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));
    if (!user.isEnabled()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
    }
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
    }
    if (expectedRole != null && !expectedRole.isBlank()) {
      Role expected = parseExpectedRole(expectedRole);
      if (user.getRole() != expected) {
        throw new BusinessException(ErrorCode.FORBIDDEN,
            "该账号是「" + roleLabel(user.getRole()) + "」账号，请前往对应的登录入口");
      }
    }
    AuthUser authUser = new AuthUser(user.getId(), user.getUsername(), user.getRole());
    String token = jwtService.issueToken(user.getId(), user.getUsername(), user.getRole());
    return new AuthToken(token, authUser);
  }

  private Role parseExpectedRole(String expectedRole) {
    try {
      return Role.valueOf(expectedRole.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "登录入口无效");
    }
  }

  private String roleLabel(Role role) {
    return switch (role) {
      case CANDIDATE -> "求职者";
      case INTERVIEWER -> "面试官/就业指导教师";
      case ADMIN -> "管理员";
    };
  }

  public AuthUser register(String username, String password, String displayName) {
    if (username == null || username.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名不能为空");
    }
    if (password == null || password.length() < 6 || password.length() > 64) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "密码长度需在 6-64 位之间");
    }
    if (userRepository.existsByUsername(username)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在");
    }
    UserEntity user = new UserEntity();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setRole(Role.CANDIDATE);
    user.setDisplayName(displayName);
    user.setEnabled(true);
    userRepository.save(user);
    return new AuthUser(user.getId(), user.getUsername(), user.getRole());
  }
}