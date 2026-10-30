package interview.guide.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamMessageId;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.RedisException;

/**
 * Redis Stream 消费在 Redis &lt; 6.2（无 XAUTOCLAIM 命令）时的降级行为测试。
 */
@DisplayName("Redis Stream 消费降级（XAUTOCLAIM 不可用）")
class RedisServiceAutoClaimFallbackTest {

  @Test
  @DisplayName("Redis 不支持 XAUTOCLAIM 时跳过 Pending 回收，仍继续消费新消息")
  void skipsAutoClaimAndReadsNewMessages() {
    RedissonClient client = mock(RedissonClient.class);
    RStream<String, String> stream = mock(RStream.class);
    when(client.<String, String>getStream(anyString(), any())).thenReturn(stream);

    // 模拟 Redis 5.x：XAUTOCLAIM 抛 unknown command
    when(stream.autoClaim(anyString(), anyString(), anyLong(), any(), any(), anyInt()))
        .thenThrow(new RedisException("ERR unknown command `XAUTOCLAIM`, with args beginning with: "
            + "`s:stream`, `group`, ..."));

    StreamMessageId id = new StreamMessageId(1L);
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("sessionId", "s1");
    when(stream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
        .thenReturn(Map.of(id, fields));

    RedisService service = new RedisService(client);
    Map<StreamMessageId, Map<String, String>> processed = new LinkedHashMap<>();
    boolean result = service.streamConsumeMessages(
        "s:stream", "group", "consumer-1",
        10, 1000L, 300_000L, 10,
        (messageId, data) -> processed.put(messageId, data)
    );

    assertThat(result).isTrue();
    assertThat(processed).containsKey(id);
    verify(stream).readGroup(eq("group"), eq("consumer-1"), any(StreamReadGroupArgs.class));
  }

  @Test
  @DisplayName("XAUTOCLAIM 降级后无新消息时不处理任何消息")
  void noNewMessagesAfterFallback() {
    RedissonClient client = mock(RedissonClient.class);
    RStream<String, String> stream = mock(RStream.class);
    when(client.<String, String>getStream(anyString(), any())).thenReturn(stream);
    when(stream.autoClaim(anyString(), anyString(), anyLong(), any(), any(), anyInt()))
        .thenThrow(new RedisException("ERR unknown command `XAUTOCLAIM`"));
    when(stream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
        .thenReturn(Map.of());

    RedisService service = new RedisService(client);
    boolean result = service.streamConsumeMessages(
        "s:stream", "group", "consumer-1",
        10, 1000L, 300_000L, 10,
        (messageId, data) -> {
        }
    );

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("其它 Redis 异常（如连接错误）不被吞掉，继续向上抛出")
  void otherRedisExceptionPropagates() {
    RedissonClient client = mock(RedissonClient.class);
    RStream<String, String> stream = mock(RStream.class);
    when(client.<String, String>getStream(anyString(), any())).thenReturn(stream);
    when(stream.autoClaim(anyString(), anyString(), anyLong(), any(), any(), anyInt()))
        .thenThrow(new RedisException("Unable to connect to Redis server"));
    when(stream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
        .thenReturn(Map.of());

    RedisService service = new RedisService(client);
    try {
      service.streamConsumeMessages(
          "s:stream", "group", "consumer-1",
          10, 1000L, 300_000L, 10,
          (messageId, data) -> {
          }
      );
    } catch (RedisException e) {
      assertThat(e.getMessage()).contains("Unable to connect");
      return;
    }
    throw new AssertionError("应抛出连接类 RedisException");
  }
}