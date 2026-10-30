package interview.guide.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 演示账号种子数据：仅当 app.auth.seed-demo-users=true 且 users 表为空时创建。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoUserSeeder implements ApplicationRunner {

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  @Value("${app.auth.seed-demo-users:false}")
  private boolean seedDemoUsers;

  @Override
  public void run(ApplicationArguments args) {
    if (!seedDemoUsers) {
      return;
    }
    if (userRepository.count() > 0) {
      log.info("users 表已有数据，跳过演示账号种子");
      return;
    }
    seed("candidate", "candidate123", Role.CANDIDATE, "候选同学A");
    seed("interviewer", "interviewer123", Role.INTERVIEWER, "就业指导李老师");
    seed("admin", "admin123", Role.ADMIN, "平台管理员");
    log.warn("演示账号已创建（candidate/interviewer/admin），仅用于开发环境，生产环境请关闭 app.auth.seed-demo-users");
  }

  private void seed(String username, String rawPassword, Role role, String displayName) {
    UserEntity user = new UserEntity();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(rawPassword));
    user.setRole(role);
    user.setDisplayName(displayName);
    user.setEnabled(true);
    userRepository.save(user);
  }
}