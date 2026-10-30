package interview.guide.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT 鉴权过滤器：白名单放行，其余请求校验 Bearer token。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthFilter implements Filter {

  private static final List<String> WHITELIST_PREFIXES = List.of(
      "/api/v1/auth/login",
      "/api/v1/auth/register",
      "/actuator/",
      "/swagger-ui",
      "/v3/api-docs",
      "/api/interview/skills",
      "/api/interview/demo-mode"
  );

  /** 仅管理员可访问的路径前缀（AI Provider 配置与语音服务设置） */
  private static final List<String> ADMIN_ONLY_PREFIXES = List.of(
      "/api/llm-provider/"
  );

  private static final String UNAUTHORIZED_BODY =
      "{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null,\"success\":false}";

  private static final String FORBIDDEN_BODY =
      "{\"code\":403,\"message\":\"无权限\",\"data\":null,\"success\":false}";

  private final JwtService jwtService;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
      chain.doFilter(request, response);
      return;
    }

    String path = httpRequest.getRequestURI();
    if (isWhitelisted(path)) {
      chain.doFilter(request, response);
      return;
    }

    String authHeader = httpRequest.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      writeUnauthorized(httpResponse);
      return;
    }

    String token = authHeader.substring("Bearer ".length()).trim();
    Optional<AuthUser> authUser = jwtService.verify(token);
    if (authUser.isEmpty()) {
      writeUnauthorized(httpResponse);
      return;
    }

    httpRequest.setAttribute("authUser", authUser.get());
    // 管理员专属路径守卫：普通用户/面试官不得管理模型密钥与服务配置
    if (ADMIN_ONLY_PREFIXES.stream().anyMatch(path::startsWith)
        && authUser.get().role() != Role.ADMIN) {
      httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
      httpResponse.setContentType("application/json;charset=UTF-8");
      httpResponse.getWriter().write(FORBIDDEN_BODY);
      return;
    }
    chain.doFilter(request, response);
  }

  private boolean isWhitelisted(String path) {
    return WHITELIST_PREFIXES.stream().anyMatch(path::startsWith);
  }

  private void writeUnauthorized(HttpServletResponse httpResponse) throws IOException {
    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    httpResponse.setContentType("application/json;charset=UTF-8");
    httpResponse.getWriter().write(UNAUTHORIZED_BODY);
  }
}