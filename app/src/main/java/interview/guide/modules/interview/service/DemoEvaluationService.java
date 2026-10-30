package interview.guide.modules.interview.service;

import interview.guide.common.config.DemoModeProperties;
import interview.guide.common.evaluation.EvaluationReport;
import interview.guide.common.evaluation.EvaluationReport.CategoryScore;
import interview.guide.common.evaluation.EvaluationReport.QuestionEvaluation;
import interview.guide.common.evaluation.EvaluationReport.ReferenceAnswer;
import interview.guide.common.evaluation.QaRecord;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 本地演示模式评估服务
 *
 * <p>仅在演示模式开启时使用（{@link DemoModeProperties#isEnabled()}）。
 * 评估逻辑完全确定、可重复：回答完整度分层评分 + 分类考察点关键词命中加分，
 * 并在反馈中给出"评价依据"（回答原文证据句），不调用任何 LLM。
 *
 * <p>该服务产出与 {@code UnifiedEvaluationService} 相同的 {@link EvaluationReport}
 * 结构，报告照常写入数据库与 Redis 会话缓存，仅评估来源不同。
 */
@Service
public class DemoEvaluationService {

  private static final String DEMO_PREFIX = "【本地演示模式】";

  /** 分类考察点关键词（小写），用于演示评分与依据提取 */
  private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.ofEntries(
      Map.entry("JAVA", List.of("jvm", "虚拟机", "内存", "垃圾回收", "gc", "多线程", "线程池", "锁",
          "并发", "volatile", "synchronized", "集合", "hashmap", "类加载", "反射", "异常", "泛型")),
      Map.entry("MYSQL", List.of("索引", "b+树", "事务", "隔离级别", "锁", "mvcc", "慢查询", "优化",
          "分库分表", "主从", "redo", "undo", "执行计划")),
      Map.entry("REDIS", List.of("缓存", "穿透", "击穿", "雪崩", "过期", "淘汰", "持久化", "rdb",
          "aof", "哨兵", "集群", "分布式锁", "数据结构", "zset")),
      Map.entry("SPRING", List.of("ioc", "aop", "bean", "事务", "自动装配", "启动流程", "springboot",
          "spring mvc", "依赖注入", "循环依赖", "代理")),
      Map.entry("PROJECT", List.of("背景", "职责", "难点", "方案", "架构", "压测", "监控", "上线",
          "复盘", "优化", "指标")),
      Map.entry("SYSTEM_DESIGN_SCENARIO", List.of("高并发", "限流", "降级", "熔断", "幂等", "一致性",
          "分布式", "消息队列", "设计", "扩展", "容量", "瓶颈")),
      Map.entry("AGENT 基础", List.of("agent", "智能体", "llm", "大模型", "mcp", "工具调用", "rag",
          "检索增强", "记忆", "上下文", "规划", "多agent", "多智能体", "thinking", "思考", "行动"))
  );

  /** 未配置专属关键词的分类使用通用能力点，保证任意技能均可提取依据、不编造（只取自回答原文） */
  private static final List<String> GENERIC_KEYWORDS = List.of(
      "原理", "机制", "流程", "架构", "设计", "实践", "优化", "案例", "场景", "实现");

  private static final List<String> GENERIC_KEY_POINTS = List.of(
      "核心概念与原理", "关键流程与机制", "优点与局限", "典型应用场景");

  private final DemoModeProperties demoProperties;

  public DemoEvaluationService(DemoModeProperties demoProperties) {
    this.demoProperties = demoProperties;
  }

  /**
   * 确定性评估全部问答记录。
   */
  public EvaluationReport evaluate(String sessionId, List<QaRecord> qaRecords) {
    List<QuestionEvaluation> questionDetails = new ArrayList<>();
    List<ReferenceAnswer> referenceAnswers = new ArrayList<>();
    Map<String, List<Integer>> categoryScores = new LinkedHashMap<>();

    for (QaRecord qa : qaRecords) {
      DemoScore score = scoreAnswer(qa);
      questionDetails.add(new QuestionEvaluation(
          qa.questionIndex(), qa.question(), qa.category(), qa.userAnswer(),
          score.total(), score.feedback(), score.evidence()
      ));

      String baseCategory = baseCategory(qa.category());
      referenceAnswers.add(new ReferenceAnswer(
          qa.questionIndex(), qa.question(),
          buildReferenceAnswer(baseCategory),
          keyPointsFor(baseCategory)
      ));
      categoryScores.computeIfAbsent(baseCategory, k -> new ArrayList<>()).add(score.total());
    }

    List<CategoryScore> categoryScoreList = categoryScores.entrySet().stream()
        .map(e -> new CategoryScore(e.getKey(),
            (int) e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0),
            e.getValue().size()))
        .toList();

    long answeredCount = qaRecords.stream()
        .filter(q -> q.userAnswer() != null && !q.userAnswer().isBlank())
        .count();
    int overallScore = answeredCount == 0 ? 0
        : (int) questionDetails.stream().mapToInt(QuestionEvaluation::score).average().orElse(0);

    return new EvaluationReport(
        sessionId,
        qaRecords.size(),
        overallScore,
        categoryScoreList,
        questionDetails,
        buildOverallFeedback(qaRecords.size(), overallScore, categoryScoreList),
        buildStrengths(categoryScoreList, overallScore),
        buildImprovements(categoryScoreList),
        referenceAnswers
    );
  }

  /**
   * 确定性评分：回答完整度分层 + 关键词命中加分（上限 15 分），满分 100。
   */
  private DemoScore scoreAnswer(QaRecord qa) {
    String answer = qa.userAnswer() == null ? "" : qa.userAnswer().trim();
    if (answer.isEmpty()) {
      return new DemoScore(0,
          DEMO_PREFIX + "未作答，本题计 0 分。请结合该方向的关键概念与自身实践作答。", null);
    }

    int lengthTier = lengthTier(answer.length());
    List<String> keywords = keywordsFor(baseCategory(qa.category()));
    Set<String> hits = hitKeywords(answer, keywords);
    int keywordBonus = Math.min(15, hits.size() * 5);
    List<String> evidences = extractEvidences(answer, keywords);

    StringBuilder feedback = new StringBuilder()
        .append(DEMO_PREFIX)
        .append("回答约 ").append(answer.length()).append(" 字（完整度基础分 ")
        .append(lengthTier).append("）；")
        .append("命中考察点 ").append(hits.size()).append(" 个");
    if (!hits.isEmpty()) {
      feedback.append("（").append(String.join("、", hits)).append("）");
    }
    feedback.append("，考察点加分 ").append(keywordBonus).append("。");
    if (evidences.isEmpty()) {
      feedback.append("未提取到明确的考察点依据句，建议围绕分类关键词展开论述。");
    } else {
      feedback.append("\n评价依据：\n");
      for (String sentence : evidences) {
        feedback.append("· 「").append(sentence).append("」\n");
      }
    }
    return new DemoScore(Math.min(100, lengthTier + keywordBonus), feedback.toString(),
        buildEvidence(evidences));
  }

  /**
   * 组装证据字段：最多拼接 2 条证据句，超出截断，保持可读。
   */
  private String buildEvidence(List<String> evidences) {
    List<String> limited = evidences.stream().limit(2).toList();
    if (limited.isEmpty()) {
      return null;
    }
    String joined = String.join("；", limited);
    return joined.length() > 200 ? joined.substring(0, 200) + "…" : joined;
  }

  private int lengthTier(int length) {
    if (length < 10) {
      return 15;
    }
    if (length < 50) {
      return 35;
    }
    if (length < 120) {
      return 55;
    }
    if (length < 300) {
      return 70;
    }
    return 85;
  }

  /**
   * 命中关键词（小写、子串匹配），保持映射表顺序、去重。
   */
  private Set<String> hitKeywords(String answer, List<String> keywords) {
    String normalized = answer.toLowerCase(Locale.ROOT);
    Set<String> hits = new LinkedHashSet<>();
    for (String keyword : keywords) {
      if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
        hits.add(keyword);
      }
    }
    return hits;
  }

  /**
   * 提取包含命中关键词的回答原文句子作为"评价依据"，最多 3 句。
   */
  private List<String> extractEvidences(String answer, List<String> keywords) {
    String[] sentences = answer.split("[。！？\\n；;]+");
    List<String> evidences = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (String sentence : sentences) {
      String trimmed = sentence.trim();
      if (trimmed.isEmpty() || trimmed.length() > 80) {
        continue;
      }
      String lower = trimmed.toLowerCase(Locale.ROOT);
      boolean matched = keywords.stream()
          .anyMatch(k -> lower.contains(k.toLowerCase(Locale.ROOT)));
      if (matched && seen.add(trimmed) && evidences.size() < 3) {
        evidences.add(trimmed);
      }
    }
    return evidences;
  }

  private String buildReferenceAnswer(String baseCategory) {
    List<String> points = keyPointsFor(baseCategory);
    return "参考答案（演示）：建议围绕「" + baseCategory + "」方向的关键考察点作答，例如："
        + String.join("、", points) + "，并尽量结合一个真实项目场景说明。";
  }

  private List<String> keyPointsFor(String baseCategory) {
    List<String> keywords = keywordsFor(baseCategory);
    if (keywords.isEmpty()) {
      return GENERIC_KEY_POINTS;
    }
    return keywords.stream().limit(4).toList();
  }

  private List<String> keywordsFor(String baseCategory) {
    List<String> keywords = CATEGORY_KEYWORDS.get(baseCategory);
    return keywords == null ? GENERIC_KEYWORDS : keywords;
  }

  private String buildOverallFeedback(int totalQuestions, int overallScore,
                                      List<CategoryScore> categoryScores) {
    return DEMO_PREFIX + "本次共 " + totalQuestions + " 题，平均分 " + overallScore
        + "。本报告由本地确定性演示逻辑生成，未调用真实模型推理，仅用于演示"
        + "系统完整业务闭环（出题、作答、状态流转、评估、落库与报告展示）。";
  }

  private List<String> buildStrengths(List<CategoryScore> categoryScores, int overallScore) {
    List<String> strengths = new ArrayList<>();
    for (CategoryScore cs : categoryScores) {
      if (cs.score() >= 70) {
        strengths.add("在「" + cs.category() + "」方向回答覆盖较完整（平均分 " + cs.score() + "）。");
      }
    }
    if (strengths.isEmpty() && overallScore >= 50) {
      strengths.add("整体作答认真，具备基本的论述结构。");
    }
    if (strengths.isEmpty()) {
      strengths.add("暂无明显优势项，建议先围绕关键考察点展开论述。");
    }
    return strengths;
  }

  private List<String> buildImprovements(List<CategoryScore> categoryScores) {
    List<String> improvements = new ArrayList<>();
    for (CategoryScore cs : categoryScores) {
      if (cs.score() < 50) {
        improvements.add("在「" + cs.category() + "」方向建议补充关键概念、机制原理与一次真实实践。");
      }
    }
    improvements.add("（演示模式提示）本报告由本地确定性逻辑生成，仅供演示参考。");
    return improvements;
  }

  /**
   * 归一化分类名：去掉"（追问N）"等后缀并转大写，用于关键词匹配与类别聚合。
   */
  private String baseCategory(String category) {
    if (category == null || category.isBlank()) {
      return "GENERAL";
    }
    String base = category;
    int paren = base.indexOf('（');
    if (paren > 0) {
      base = base.substring(0, paren);
    }
    return base.trim().toUpperCase(Locale.ROOT);
  }

  private record DemoScore(int total, String feedback, String evidence) {}
}