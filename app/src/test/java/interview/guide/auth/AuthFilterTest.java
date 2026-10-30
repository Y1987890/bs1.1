package interview.guide.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("AuthFilter 测试")
class AuthFilterTest {

  private final JwtService jwtService = mock(JwtService.class);
  private final AuthFilter filter = new AuthFilter(jwtService);

  @Test
  @DisplayName("白名单路径放行")
  void whitelistedPathPasses() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  @DisplayName("无 token 返回 401")
  void noTokenReturns401() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(chain.getRequest()).isNull();
    assertThat(response.getContentAsString()).contains("\"code\":401");
  }

  @Test
  @DisplayName("坏 token 返回 401")
  void badTokenReturns401() throws Exception {
    when(jwtService.verify(eq("bad-token"))).thenReturn(Optional.empty());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
    request.addHeader("Authorization", "Bearer bad-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  @DisplayName("好 token 注入 authUser attribute 并放行")
  void goodTokenInjectsAttribute() throws Exception {
    AuthUser authUser = new AuthUser(1L, "admin", Role.ADMIN);
    when(jwtService.verify(eq("good-token"))).thenReturn(Optional.of(authUser));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
    request.addHeader("Authorization", "Bearer good-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
    assertThat(request.getAttribute("authUser")).isEqualTo(authUser);
  }
}