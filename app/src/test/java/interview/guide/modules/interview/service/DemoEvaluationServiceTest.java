package interview.guide.modules.interview.service;

import static org.assertj.core.api.Assertions.assertThat;

import interview.guide.common.config.DemoModeProperties;
import interview.guide.common.evaluation.EvaluationReport;
import interview.guide.common.evaluation.QaRecord;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 本地演示模式评估服务测试（纯单元测试，无 Spring 上下文）
 */
@DisplayName("本地演示模式评估服务")
class DemoEvaluationServiceTest {

  private final DemoEvaluationService service =
      new DemoEvaluationService(new DemoModeProperties());

  @Nested
  @DisplayName("确定性评分")
  class Scoring {

    @Test
    @DisplayName("未作答计 0 分")
    void blankAnswerScoresZero() {
      QaRecord qa = new QaRecord(0, "请介绍 JVM 内存模型", "JAVA", "");
      EvaluationReport report = service.evaluate("s1", List.of(qa));

      assertThat(report.questionDetails()).hasSize(1);
      assertThat(report.questionDetails().get(0).score()).isZero();
      assertThat(report.questionDetails().get(0).feedback()).contains("未作答");
      assertThat(report.overallScore()).isZero();
    }

    @Test
    @DisplayName("回答完整度分层与关键词命中加分，满分封顶 100")
    void keywordHitsAddScoreCappedAt100() {
      String longAnswer = String.join("。",
          "JVM 的内存分为堆、栈、方法区等区域",
          "垃圾回收使用标记清除与复制算法，存在 GC 停顿",
          "线程池可以复用线程降低创建开销",
          "volatile 保证可见性但不保证原子性",
          "synchronized 是可重入锁。");
      QaRecord qa = new QaRecord(0, "请介绍 JVM 内存模型", "JAVA", longAnswer);
      EvaluationReport report = service.evaluate("s1", List.of(qa));

      assertThat(report.questionDetails().get(0).score()).isLessThanOrEqualTo(100);
      assertThat(report.questionDetails().get(0).feedback()).contains("命中考察点");
    }

    @Test
    @DisplayName("同一输入多次评估结果完全一致（确定性）")
    void deterministicEvaluation() {
      QaRecord qa = new QaRecord(0, "谈谈 Redis 缓存", "REDIS",
          "使用缓存穿透时需要对空值缓存或使用布隆过滤器，缓存雪崩可以通过过期时间加随机值缓解。");
      EvaluationReport first = service.evaluate("s1", List.of(qa));
      EvaluationReport second = service.evaluate("s1", List.of(qa));

      assertThat(first.overallScore()).isEqualTo(second.overallScore());
      assertThat(first.overallFeedback()).isEqualTo(second.overallFeedback());
      assertThat(first.questionDetails().get(0).feedback())
          .isEqualTo(second.questionDetails().get(0).feedback());
    }
  }

  @Nested
  @DisplayName("评价依据提取")
  class Evidence {

    @Test
    @DisplayName("反馈中包含命中关键词的回答原文证据句")
    void feedbackQuotesEvidenceSentence() {
      QaRecord qa = new QaRecord(0, "谈谈你对垃圾回收的理解", "JAVA",
          "我对垃圾回收的理解是，GC 通过可达性分析判断对象是否存活，再通过分代收集回收内存。");
      EvaluationReport report = service.evaluate("s1", List.of(qa));

      String feedback = report.questionDetails().get(0).feedback();
      assertThat(feedback).contains("评价依据");
      assertThat(feedback).contains("GC 通过可达性分析");
    }

    @Test
    @DisplayName("无关键词命中时不生成依据句，并给出提示")
    void noEvidenceWhenNoKeywordHit() {
      QaRecord qa = new QaRecord(0, "谈谈你的项目经历", "PROJECT",
          "我主要负责日常的页面调整和文档整理，工作内容比较杂。");
      EvaluationReport report = service.evaluate("s1", List.of(qa));

      String feedback = report.questionDetails().get(0).feedback();
      assertThat(feedback).contains("未提取到明确的考察点依据句");
    }
  }

  @Nested
  @DisplayName("类别聚合与报告结构")
  class Aggregation {

    @Test
    @DisplayName("追问类分类归一化后与主问题同类聚合")
    void followUpCategoryNormalized() {
      QaRecord main = new QaRecord(0, "主问题", "JAVA", "JVM 内存模型与垃圾回收机制需要重点掌握。");
      QaRecord followUp = new QaRecord(1, "追问", "JAVA（追问1）", "线程安全问题可以通过加锁解决。");
      EvaluationReport report = service.evaluate("s1", List.of(main, followUp));

      assertThat(report.categoryScores())
          .anyMatch(cs -> cs.category().equals("JAVA") && cs.questionCount() == 2);
    }

    @Test
    @DisplayName("报告结构完整：总分、类别分、每题详情、参考要点齐备")
    void reportStructureComplete() {
      QaRecord qa = new QaRecord(0, "谈谈你对垃圾回收的理解", "JAVA",
          "GC 通过可达性分析判断对象是否存活，再通过分代收集回收内存，常见的收集器有 G1 与 CMS。");
      EvaluationReport report = service.evaluate("s1", List.of(qa));

      assertThat(report.sessionId()).isEqualTo("s1");
      assertThat(report.totalQuestions()).isEqualTo(1);
      assertThat(report.categoryScores()).hasSize(1);
      assertThat(report.strengths()).isNotEmpty();
      assertThat(report.improvements()).anyMatch(i -> i.contains("演示模式提示"));
      assertThat(report.referenceAnswers().get(0).keyPoints()).isNotEmpty();
      assertThat(report.overallFeedback()).contains("本地演示模式");
    }
  }

  @Test
  @DisplayName("演示模式配置默认关闭")
  void demoModeDefaultsToDisabled() {
    assertThat(new DemoModeProperties().isEnabled()).isFalse();
  }
}