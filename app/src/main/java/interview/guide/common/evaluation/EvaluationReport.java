package interview.guide.common.evaluation;

import java.util.List;

/**
 * 通用面试评估报告（文字面试和语音面试共用）
 */
public record EvaluationReport(
    String sessionId,
    int totalQuestions,
    int overallScore,
    List<CategoryScore> categoryScores,
    List<QuestionEvaluation> questionDetails,
    String overallFeedback,
    List<String> strengths,
    List<String> improvements,
    List<ReferenceAnswer> referenceAnswers
) {
    public record CategoryScore(
        String category,
        int score,
        int questionCount
    ) {}

    public record QuestionEvaluation(
        int questionIndex,
        String question,
        String category,
        String userAnswer,
        int score,
        String feedback,
        String evidence   // 候选人回答中的支持证据原文；AI 模式无法产出时为 null（前端显示"信息不足"，禁止编造）
    ) {}

    public record ReferenceAnswer(
        int questionIndex,
        String question,
        String referenceAnswer,
        List<String> keyPoints
    ) {}
}
