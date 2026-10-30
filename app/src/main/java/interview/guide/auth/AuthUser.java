package interview.guide.auth;

/**
 * 已认证用户信息（脱敏，不携带密码）
 */
public record AuthUser(Long userId, String username, Role role) {}