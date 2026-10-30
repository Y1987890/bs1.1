package interview.guide.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "用户名不能为空") String username,
    @NotBlank(message = "密码不能为空") String password,
    /** 可选：登录入口期望的角色（三角色独立登录入口用）；为空表示不限制 */
    String expectedRole) {

  public LoginRequest(String username, String password) {
    this(username, password, null);
  }
}
