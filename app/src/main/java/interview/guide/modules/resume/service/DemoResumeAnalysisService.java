package interview.guide.modules.resume.service;

import interview.guide.common.config.DemoModeProperties;
import interview.guide.modules.interview.model.ResumeAnalysisResponse;
import interview.guide.modules.interview.model.ResumeAnalysisResponse.ScoreDetail;
import interview.guide.modules.interview.model.ResumeAnalysisResponse.Suggestion;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 本地演示模式简历分析服务
 *
 * <p>仅在演示模式开启时使用（{@link DemoModeProperties#isEnabled()}）。
 * 分析逻辑完全确定、可重复：按常见简历段落（工作/项目/技能/教育等）与技能关键词
 * 覆盖率给出维度评分，并在优点/建议中引用简历原文证据句。不调用任何 LLM。
 */
@Service
public class DemoResumeAnalysisService {

  private static final String DEMO_PREFIX = "【本地演示模式】";

  private static final List<String> SECTION_KEYWORDS = List.of(
      "工作经历", "工作经验", "项目经历", "项目经验", "技能", "专业技能",
      "教育", "学历", "自我评价", "个人总结", "获奖", "证书");

  private static final List<String> SKILL_KEYWORDS = List.of(
      "java", "spring", "spring boot", "mysql", "redis", "分布式", "消息队列",
      "微服务", "linux", "docker", "数据库", "并发", "多线程", "python", "前端",
      "vue", "react", "javascript", "golang", "kafka", "es", "elasticsearch");

  private static final List<String> PROJECT_KEYWORDS = List.of(
      "项目", "负责", "参与", "优化", "重构", "上线", "压测", "指标", "方案",
      "qps", "接口耗时", "性能");

  private final DemoModeProperties demoModeProperties;

  public DemoResumeAnalysisService(DemoModeProperties demoModeProperties) {
    this.demoModeProperties = demoModeProperties;
  }

  /**
   * 确定性分析简历文本。
   */
  public ResumeAnalysisResponse analyze(String resumeText) {
    String text = resumeText == null ? "" : resumeText.trim();
    List<String> sections = findSections(text);
    Set<String> skills = hitKeywords(text, SKILL_KEYWORDS);
    Set<String> projectHits = hitKeywords(text, PROJECT_KEYWORDS);

    int contentScore = Math.min(25, sections.size() * 3 + Math.min(10, text.length() / 20));
    int structureScore = Math.min(20, sections.size() * 3 + (hasContact(text) ? 1 : 0));
    int skillMatchScore = Math.min(25, skills.size() * 2);
    int expressionScore = Math.min(15, 6 + Math.min(9, text.length() / 30));
    int projectScore = Math.min(15, projectHits.size() * 2);
    int overallScore = contentScore + structureScore + skillMatchScore + expressionScore + projectScore;

    ScoreDetail scoreDetail = new ScoreDetail(
        contentScore, structureScore, skillMatchScore, expressionScore, projectScore);

    List<String> strengths = buildStrengths(sections, skills, projectHits, text);
    List<Suggestion> suggestions = buildSuggestions(sections, skills, text);
    String summary = buildSummary(text, overallScore, sections.size(), skills.size());

    return new ResumeAnalysisResponse(overallScore, scoreDetail, summary, strengths, suggestions, resumeText);
  }

  private String buildSummary(String text, int overallScore, int sectionCount, int skillCount) {
    return DEMO_PREFIX + "本报告由本地确定性演示逻辑生成，未调用真实模型推理，仅用于演示简历分析业务流程。"
        + "文本约 " + text.length() + " 字，识别出 " + sectionCount + " 个常见段落，命中 "
        + skillCount + " 个技能关键词，总分 " + overallScore + "/100。";
  }

  private List<String> buildStrengths(List<String> sections, Set<String> skills,
                                      Set<String> projectHits, String text) {
    List<String> strengths = new ArrayList<>();
    if (!sections.isEmpty()) {
      strengths.add("简历结构包含以下段落：" + String.join("、", sections) + "，信息组织较清晰。");
    }
    if (!skills.isEmpty()) {
      strengths.add("技能关键词覆盖 " + skills.size() + " 项（如 "
          + String.join("、", skills.stream().limit(5).toList()) + "）。");
    }
    if (!projectHits.isEmpty()) {
      List<String> evidence = extractEvidenceLines(text, projectHits);
      strengths.add("存在项目/实践相关描述，依据：「"
          + String.join("」「", evidence.stream().limit(2).toList()) + "」。");
    }
    if (strengths.isEmpty()) {
      strengths.add("简历已有基本内容，建议按\"工作经历/项目经历/技能\"结构补充细节。");
    }
    return strengths;
  }

  private List<Suggestion> buildSuggestions(List<String> sections, Set<String> skills, String text) {
    List<Suggestion> suggestions = new ArrayList<>();
    if (!hasAnySection(sections, "项目经历", "项目经验")) {
      suggestions.add(new Suggestion("项目",
          "高", "缺少\"项目经历\"段落",
          "补充 1-2 个真实项目：背景、你的职责、技术方案与量化结果（如 QPS、耗时下降比例）。"));
    }
    if (!hasAnySection(sections, "工作经历", "工作经验")) {
      suggestions.add(new Suggestion("内容",
          "高", "缺少\"工作经历\"段落",
          "按时间倒序补充公司、岗位、在职时间与核心职责。"));
    }
    if (!hasAnySection(sections, "技能", "专业技能")) {
      suggestions.add(new Suggestion("技能",
          "中", "缺少独立\"技能\"段落",
          "单独列出技能清单（语言/框架/中间件），便于关键词匹配。"));
    }
    if (skills.size() < 5) {
      suggestions.add(new Suggestion("技能",
          "中", "技能关键词偏少（" + skills.size() + " 项）",
          "按岗位 JD 补充 Java/Spring/MySQL/Redis 等技术词，提高机器筛选命中率。"));
    }
    if (text.length() < 200) {
      suggestions.add(new Suggestion("内容",
          "低", "简历内容偏短（" + text.length() + " 字）",
          "适当补充项目细节与量化成果，控制在 1-2 页。"));
    }
    suggestions.add(new Suggestion("系统",
        "低", "（演示模式提示）分析结果来自本地确定性逻辑",
        demoModeProperties.getLabel() + "，仅供演示参考。"));
    return suggestions;
  }

  private List<String> findSections(String text) {
    List<String> found = new ArrayList<>();
    for (String keyword : SECTION_KEYWORDS) {
      if (text.contains(keyword) && found.stream().noneMatch(s -> s.equals(keyword))) {
        found.add(keyword);
      }
    }
    return found;
  }

  private boolean hasAnySection(List<String> sections, String... candidates) {
    for (String candidate : candidates) {
      if (sections.contains(candidate)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasContact(String text) {
    return text.contains("@") || text.contains("电话") || text.contains("手机");
  }

  private Set<String> hitKeywords(String text, List<String> keywords) {
    String lower = text.toLowerCase(Locale.ROOT);
    Set<String> hits = new LinkedHashSet<>();
    for (String keyword : keywords) {
      if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
        hits.add(keyword);
      }
    }
    return hits;
  }

  private List<String> extractEvidenceLines(String text, Set<String> keywords) {
    List<String> lines = new ArrayList<>();
    for (String line : text.split("[\\n；;]+")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.length() > 80) {
        continue;
      }
      String lower = trimmed.toLowerCase(Locale.ROOT);
      if (keywords.stream().anyMatch(k -> lower.contains(k.toLowerCase(Locale.ROOT)))) {
        lines.add(trimmed);
      }
    }
    return lines;
  }
}