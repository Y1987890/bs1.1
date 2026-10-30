package interview.guide.auth;

/**
 * 登录成功后返回的 token 与用户信息
 */
public record AuthToken(String token, AuthUser user) {}