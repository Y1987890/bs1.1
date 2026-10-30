package interview.guide.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import interview.guide.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@DisplayName("AuthSupport 测试")
class AuthSupportTest {

  @Test
  @DisplayName("未登录抛出异常")
  void requireAuthWithoutLoginThrows() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThatThrownBy(() -> AuthSupport.requireAuth(request))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", 401);
  }

  @Test
  @DisplayName("角色不匹配抛出 403")
  void roleMismatchThrows() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("authUser", new AuthUser(1L, "candidate", Role.CANDIDATE));

    assertThatThrownBy(() -> AuthSupport.requireAnyRole(request, Role.ADMIN))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", 403);
  }

  @Test
  @DisplayName("角色匹配通过")
  void roleMatchPasses() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("authUser", new AuthUser(1L, "admin", Role.ADMIN));

    assertThat(AuthSupport.requireAnyRole(request, Role.ADMIN).username()).isEqualTo("admin");
    assertThat(AuthSupport.requireAuth(request).role()).isEqualTo(Role.ADMIN);
    assertThat(AuthSupport.hasRole(request, Role.ADMIN)).isTrue();
    assertThat(AuthSupport.hasRole(request, Role.CANDIDATE)).isFalse();
  }

  @Test
  @DisplayName("未登录 hasRole 返回 false 而不抛异常")
  void hasRoleWithoutLoginReturnsFalse() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThat(AuthSupport.hasRole(request, Role.ADMIN)).isFalse();
  }
}