package interview.guide.modules.interview.service;

import interview.guide.common.config.DemoModeProperties;
import interview.guide.common.evaluation.EvaluationReport;
import interview.guide.common.evaluation.QaRecord;
import interview.guide.common.evaluation.UnifiedEvaluationService;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.interview.model.InterviewQuestionDTO;
import interview.guide.modules.interview.model.InterviewReportDTO;
import interview.guide.modules.interview.model.InterviewReportDTO.CategoryScore;
import interview.guide.modules.interview.model.InterviewReportDTO.QuestionEvaluation;
import interview.guide.modules.interview.model.InterviewReportDTO.ReferenceAnswer;
import interview.guide.modules.interview.skill.InterviewSkillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文字面试答案评估服务
 * 职责：DTO 适配器，将 InterviewQuestionDTO 转为通用 QaRecord，调用 UnifiedEvaluationService
 */
@Service
public class AnswerEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(AnswerEvaluationService.class);

    private final UnifiedEvaluationService unifiedEvaluationService;
    private final InterviewPersistenceService persistenceService;
    private final InterviewSkillService skillService;
    private final DemoEvaluationService demoEvaluationService;
    private final DemoModeProperties demoModeProperties;

    public AnswerEvaluationService(UnifiedEvaluationService unifiedEvaluationService,
                                   InterviewPersistenceService persistenceService,
                                   InterviewSkillService skillService,
                                   DemoEvaluationService demoEvaluationService,
                                   DemoModeProperties demoModeProperties) {
        this.unifiedEvaluationService = unifiedEvaluationService;
        this.persistenceService = persistenceService;
        this.skillService = skillService;
        this.demoEvaluationService = demoEvaluationService;
        this.demoModeProperties = demoModeProperties;
    }

    /**
     * 演示模式是否启用（不调用 LLM，走本地确定性评估）。
     */
    public boolean isDemoModeEnabled() {
        return demoModeProperties.isEnabled();
    }

    /**
     * 演示模式评估：无需 ChatClient，使用本地确定性逻辑。
     */
    public InterviewReportDTO evaluateInterviewInDemoMode(String sessionId, String resumeText,
                                                          List<InterviewQuestionDTO> questions) {
        log.info("演示模式评估面试: {}, 共{}题", sessionId, questions.size());
        List<QaRecord> qaRecords = questions.stream()
            .map(q -> new QaRecord(q.questionIndex(), q.question(), q.category(), q.userAnswer()))
            .toList();
        EvaluationReport report = demoEvaluationService.evaluate(sessionId, qaRecords);
        InterviewReportDTO dto = toInterviewReportDTO(report);
        return new InterviewReportDTO(
            dto.sessionId(), dto.totalQuestions(), dto.overallScore(), dto.categoryScores(),
            dto.questionDetails(),
            demoModeProperties.getLabel() + "\n\n" + dto.overallFeedback(),
            dto.strengths(), dto.improvements(),
            dto.referenceAnswers()
        );
    }

    /**
     * 评估完整面试并生成报告
     */
    public InterviewReportDTO evaluateInterview(ChatClient chatClient, String sessionId, String resumeText,
                                                 List<InterviewQuestionDTO> questions) {
        log.info("开始评估面试: {}, 共{}题", sessionId, questions.size());

        try {
            // 转为通用问答记录
            List<QaRecord> qaRecords = questions.stream()
                .map(q -> new QaRecord(q.questionIndex(), q.question(), q.category(), q.userAnswer()))
                .toList();

            String referenceContext = skillService.buildEvaluationReferenceSectionSafe(
                persistenceService.findBySessionId(sessionId)
                    .map(s -> s.getSkillId())
                    .orElse(null)
            );

            // 调用通用评估服务
            EvaluationReport report = unifiedEvaluationService.evaluate(
                chatClient, sessionId, qaRecords, resumeText, referenceContext
            );

            // 转为文字面试专用 DTO
            return toInterviewReportDTO(report);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("面试评估失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERVIEW_EVALUATION_FAILED,
                "面试评估失败：" + e.getMessage());
        }
    }

    private InterviewReportDTO toInterviewReportDTO(EvaluationReport report) {
        return new InterviewReportDTO(
            report.sessionId(),
            report.totalQuestions(),
            report.overallScore(),
            report.categoryScores().stream()
                .map(cs -> new CategoryScore(cs.category(), cs.score(), cs.questionCount()))
                .toList(),
            report.questionDetails().stream()
                .map(qe -> new QuestionEvaluation(qe.questionIndex(), qe.question(), qe.category(),
                    qe.userAnswer(), qe.score(), qe.feedback(), qe.evidence()))
                .toList(),
            report.overallFeedback(),
            report.strengths(),
            report.improvements(),
            report.referenceAnswers().stream()
                .map(ra -> new ReferenceAnswer(ra.questionIndex(), ra.question(),
                    ra.referenceAnswer(), ra.keyPoints()))
                .toList()
        );
    }
}
