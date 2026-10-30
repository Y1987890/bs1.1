package interview.guide.auth;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 自研轻量 JWT 签发/校验（HMAC-SHA256，无第三方依赖）
 */
@Service
@Slf4j
public class JwtService {

  private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

  private final JwtProperties properties;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public JwtService(JwtProperties properties) {
    this.properties = properties;
  }

  public String issueToken(Long userId, String username, Role role) {
    return issueToken(userId, username, role, properties.getExpireMinutes());
  }

  /**
   * 包内可测的重载：接受任意过期分钟，负数即生成已过期 token。
   */
  String issueToken(Long userId, String username, Role role, int expireMinutes) {
    String header = base64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("sub", String.valueOf(userId));
    claims.put("username", username);
    claims.put("role", role.name());
    claims.put("exp", Instant.now().plusSeconds(expireMinutes * 60L).getEpochSecond());
    String payload = base64Url(writeJson(claims));
    String data = header + "." + payload;
    return data + "." + base64Url(hmacSha256(data));
  }

  public Optional<AuthUser> verify(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    String[] parts = token.split("\\.", -1);
    if (parts.length != 3) {
      return Optional.empty();
    }
    try {
      byte[] expected = hmacSha256(parts[0] + "." + parts[1]);
      byte[] actual = base64UrlDecode(parts[2]);
      if (!MessageDigest.isEqual(expected, actual)) {
        return Optional.empty();
      }
      Map<String, Object> claims = readJson(base64UrlDecode(parts[1]));
      long exp = ((Number) claims.get("exp")).longValue();
      if (exp * 1000L <= System.currentTimeMillis()) {
        return Optional.empty();
      }
      Long userId = Long.parseLong(String.valueOf(claims.get("sub")));
      String username = (String) claims.get("username");
      Role role = Role.valueOf((String) claims.get("role"));
      return Optional.of(new AuthUser(userId, username, role));
    } catch (Exception e) {
      log.debug("JWT 校验失败，按无效 token 处理", e);
      return Optional.empty();
    }
  }

  private byte[] hmacSha256(String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(hmacKey(), "HmacSHA256"));
      return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JWT 签名失败", e);
    }
  }

  private byte[] hmacKey() {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JWT 密钥初始化失败", e);
    }
  }

  private byte[] writeJson(Map<String, Object> claims) {
    try {
      return objectMapper.writeValueAsBytes(claims);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JWT 载荷序列化失败", e);
    }
  }

  private Map<String, Object> readJson(byte[] bytes) {
    try {
      return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JWT 载荷解析失败", e);
    }
  }

  private static String base64Url(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static byte[] base64UrlDecode(String input) {
    return Base64.getUrlDecoder().decode(input);
  }
}