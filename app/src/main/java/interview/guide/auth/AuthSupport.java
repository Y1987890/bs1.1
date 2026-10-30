package interview.guide.auth;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证信息读取工具：从 request attribute 中读取 AuthUser 并做鉴权。
 */
public final class AuthSupport {

  public static final String AUTH_USER_ATTRIBUTE = "authUser";

  private AuthSupport() {}

  public static AuthUser requireAuth(HttpServletRequest request) {
    Object attr = request.getAttribute(AUTH_USER_ATTRIBUTE);
    if (attr instanceof AuthUser authUser) {
      return authUser;
    }
    throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
  }

  public static AuthUser requireAnyRole(HttpServletRequest request, Role... roles) {
    AuthUser authUser = requireAuth(request);
    for (Role role : roles) {
      if (authUser.role() == role) {
        return authUser;
      }
    }
    throw new BusinessException(ErrorCode.FORBIDDEN, "无权限");
  }

  public static boolean hasRole(HttpServletRequest request, Role role) {
    Object attr = request.getAttribute(AUTH_USER_ATTRIBUTE);
    if (!(attr instanceof AuthUser authUser)) {
      return false;
    }
    return authUser.role() == role;
  }
}