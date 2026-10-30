package interview.guide.auth;

import interview.guide.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public Result<AuthToken> login(@RequestBody @Valid LoginRequest request) {
    return Result.success(
        authService.login(request.username(), request.password(), request.expectedRole()));
  }

  @PostMapping("/register")
  public Result<AuthUser> register(@RequestBody @Valid RegisterRequest request) {
    return Result.success(authService.register(request.username(), request.password(), request.displayName()));
  }

  @GetMapping("/me")
  public Result<AuthUser> me(HttpServletRequest request) {
    return Result.success(AuthSupport.requireAuth(request));
  }
}