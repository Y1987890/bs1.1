package interview.guide.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地演示模式配置
 *
 * <p>默认关闭。仅当 APP_DEMO_ENABLED=true 时启用，用于在没有真实模型服务时
 * 演示核心业务闭环（出题与评估使用确定性本地逻辑，不调用任何 LLM）。
 * 正常模型调用模式不受影响。
 */
@Component
@ConfigurationProperties(prefix = "app.demo-mode")
public class DemoModeProperties {

  /** 是否启用本地演示模式 */
  private boolean enabled = false;

  /** 演示模式标识文案（用于前端标注与报告文本） */
  private String label = "本地演示模式（确定性演示逻辑，非真实模型推理）";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }
}