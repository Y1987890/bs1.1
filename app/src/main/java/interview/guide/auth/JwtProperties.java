package interview.guide.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性（prefix: app.jwt）
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

  private String secret;
  private int expireMinutes;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public int getExpireMinutes() {
    return expireMinutes;
  }

  public void setExpireMinutes(int expireMinutes) {
    this.expireMinutes = expireMinutes;
  }
}