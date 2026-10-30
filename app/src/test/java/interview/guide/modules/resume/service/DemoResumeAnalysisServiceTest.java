package interview.guide.modules.resume.service;

import static org.assertj.core.api.Assertions.assertThat;

import interview.guide.common.config.DemoModeProperties;
import interview.guide.modules.interview.model.ResumeAnalysisResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 本地演示模式简历分析服务测试（纯单元测试，无 Spring 上下文）
 */
@DisplayName("本地演示模式简历分析服务")
class DemoResumeAnalysisServiceTest {

  private final DemoResumeAnalysisService service =
      new DemoResumeAnalysisService(new DemoModeProperties());

  private static final String FULL_RESUME = """
      张三 | 软件工程师 | 电话 138-0000-0000 | zhangsan@example.com

      工作经历
      - 某科技公司 · 后端开发（3 年），负责订单系统设计与开发。

      项目经历
      - 订单系统重构：使用 Redis 缓存与消息队列解耦，接口耗时下降 60%，QPS 提升至 2000。
      - 数据库优化：负责 MySQL 慢查询治理与索引优化。

      专业技能
      Java、Spring Boot、MySQL、Redis、Linux、Docker

      教育背景
      某大学 计算机科学与技术 本科
      """;

  @Test
  @DisplayName("完整简历：段落识别、技能命中与各维度分数符合确定性规则")
  void fullResumeAnalysis() {
    ResumeAnalysisResponse response = service.analyze(FULL_RESUME);

    assertThat(response.overallScore())
        .isEqualTo(response.scoreDetail().contentScore()
            + response.scoreDetail().structureScore()
            + response.scoreDetail().skillMatchScore()
            + response.scoreDetail().expressionScore()
            + response.scoreDetail().projectScore());
    assertThat(response.scoreDetail().contentScore()).isLessThanOrEqualTo(25);
    assertThat(response.scoreDetail().structureScore()).isLessThanOrEqualTo(20);
    assertThat(response.scoreDetail().skillMatchScore()).isGreaterThanOrEqualTo(10);
    assertThat(response.scoreDetail().projectScore()).isGreaterThanOrEqualTo(6);
    assertThat(response.strengths()).isNotEmpty();
    assertThat(response.strengths().stream().anyMatch(s -> s.contains("技能关键词覆盖"))).isTrue();
  }

  @Test
  @DisplayName("缺少项目经历的简历会给出对应高优建议")
  void missingProjectSectionGetsSuggestion() {
    String resume = "李四 | 求职意向 Java 开发\n\n技能\nJava、Spring\n\n教育背景\n某大学 本科\n";
    ResumeAnalysisResponse response = service.analyze(resume);

    assertThat(response.suggestions())
        .anyMatch(s -> s.category().equals("项目") && s.priority().equals("高"));
  }

  @Test
  @DisplayName("短简历得到内容补充建议与低分结构分")
  void shortResumeScoresLow() {
    String resume = "王五\n求职 Java 开发，熟悉 Spring。";
    ResumeAnalysisResponse response = service.analyze(resume);

    assertThat(response.scoreDetail().structureScore()).isLessThanOrEqualTo(3);
    assertThat(response.suggestions()).anyMatch(s -> s.category().equals("内容"));
  }

  @Test
  @DisplayName("同一输入多次分析结果完全一致（确定性）")
  void deterministic() {
    ResumeAnalysisResponse first = service.analyze(FULL_RESUME);
    ResumeAnalysisResponse second = service.analyze(FULL_RESUME);

    assertThat(first.overallScore()).isEqualTo(second.overallScore());
    assertThat(first.summary()).isEqualTo(second.summary());
    assertThat(first.scoreDetail()).isEqualTo(second.scoreDetail());
  }

  @Test
  @DisplayName("报告明确标注演示模式且始终包含说明性建议")
  void labeledAsDemoMode() {
    ResumeAnalysisResponse response = service.analyze(FULL_RESUME);

    assertThat(response.summary()).contains("本地演示模式");
    assertThat(response.suggestions()).anyMatch(s -> s.category().equals("系统")
        && s.issue().contains("演示模式提示"));
    assertThat(response.originalText()).isEqualTo(FULL_RESUME);
  }
}