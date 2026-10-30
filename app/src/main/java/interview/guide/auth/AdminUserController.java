package interview.guide.auth;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

  private final UserRepository userRepository;
  private final AuditLogService auditLogService;

  @GetMapping("/users")
  public Result<List<UserView>> listUsers(HttpServletRequest request) {
    AuthSupport.requireAnyRole(request, Role.ADMIN);
    List<UserView> users = userRepository.findAll().stream()
        .map(u -> new UserView(u.getId(), u.getUsername(), u.getRole(), u.getDisplayName(),
            u.isEnabled(), u.getCreatedAt()))
        .toList();
    return Result.success(users);
  }

  @PatchMapping("/users/{id}/role")
  public Result<Void> changeRole(@PathVariable Long id, @RequestBody @Valid RoleChangeRequest body,
      HttpServletRequest request) {
    AuthUser admin = AuthSupport.requireAnyRole(request, Role.ADMIN);
    UserEntity user = userRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    Role oldRole = user.getRole();
    if (oldRole == Role.ADMIN && body.role() != Role.ADMIN) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不允许把管理员降级为其它角色");
    }
    user.setRole(body.role());
    userRepository.save(user);
    auditLogService.record(admin.username(), "CHANGE_ROLE", "User", String.valueOf(id),
        "角色由 " + oldRole.name() + " 变更为 " + body.role().name());
    return Result.success();
  }

  @GetMapping("/audit-logs")
  public Result<List<AuditLogEntity>> auditLogs(HttpServletRequest request) {
    AuthSupport.requireAnyRole(request, Role.ADMIN);
    return Result.success(auditLogService.listRecent());
  }
}