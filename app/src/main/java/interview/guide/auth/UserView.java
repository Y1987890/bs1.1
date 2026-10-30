package interview.guide.auth;

import java.time.LocalDateTime;

/**
 * 用户列表视图（不包含密码哈希）
 */
public record UserView(
    Long id,
    String username,
    Role role,
    String displayName,
    boolean enabled,
    LocalDateTime createdAt) {}