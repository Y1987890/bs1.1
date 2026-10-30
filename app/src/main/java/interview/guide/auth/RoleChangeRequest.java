package interview.guide.auth;

import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(
    @NotNull(message = "角色不能为空") Role role) {}