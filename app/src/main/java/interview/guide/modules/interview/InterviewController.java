package interview.guide.modules.interview;

import interview.guide.auth.AuthSupport;
import interview.guide.auth.AuthUser;
import interview.guide.auth.Role;
import interview.guide.common.annotation.RateLimit;
import interview.guide.common.config.DemoModeProperties;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.result.Result;
import interview.guide.modules.interview.model.CreateInterviewRequest;
import interview.guide.modules.interview.model.InterviewDetailDTO;
import interview.guide.modules.interview.model.InterviewReportDTO;
import interview.guide.modules.interview.model.InterviewSessionDTO;
import interview.guide.modules.interview.model.SessionListItemDTO;
import interview.guide.modules.interview.model.SubmitAnswerRequest;
import interview.guide.modules.interview.model.SubmitAnswerResponse;
import interview.guide.modules.interview.service.InterviewHistoryService;
import interview.guide.modules.interview.service.InterviewPersistenceService;
import interview.guide.modules.interview.service.InterviewSessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 面试控制器
 * 提供模拟面试相关的API接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "模拟面试", description = "面试会话创建、问答交互与报告生成")
public class InterviewController {
    
    private final InterviewSessionService sessionService;
    private final InterviewHistoryService historyService;
    private final InterviewPersistenceService persistenceService;
    private final DemoModeProperties demoModeProperties;

    /**
     * 查询本地演示模式状态（用于前端标注评估来源）
     */
    @GetMapping("/api/interview/demo-mode")
    public Result<Map<String, Object>> demoModeStatus() {
        return Result.success(Map.of(
            "enabled", demoModeProperties.isEnabled(),
            "label", demoModeProperties.getLabel()
        ));
    }
    
    /**
     * 列出面试会话（候选人仅本人；面试官/管理员全部）
     */
    @GetMapping("/api/interview/sessions")
    public Result<List<SessionListItemDTO>> listSessions(HttpServletRequest request) {
        AuthUser user = AuthSupport.requireAuth(request);
        List<SessionListItemDTO> items = (user.role() == Role.CANDIDATE
            ? persistenceService.findByOwnerIdOrderByCreatedAtDesc(user.userId())
            : persistenceService.findAll()).stream()
            .map(SessionListItemDTO::from)
            .toList();
        return Result.success(items);
    }

    /**
     * 创建面试会话
     */
    @PostMapping("/api/interview/sessions")
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 5)
    @RateLimit(dimension = RateLimit.Dimension.IP, count = 5)
    public Result<InterviewSessionDTO> createSession(@RequestBody CreateInterviewRequest request,
                                                     HttpServletRequest httpRequest) {
        AuthUser user = AuthSupport.requireAuth(httpRequest);
        log.info("创建面试会话，题目数量: {}, owner={}", request.questionCount(), user.userId());
        InterviewSessionDTO session = sessionService.createSession(request, user.userId());
        return Result.success(session);
    }
    
    /**
     * 获取会话信息
     */
    @GetMapping("/api/interview/sessions/{sessionId}")
    public Result<InterviewSessionDTO> getSession(@PathVariable String sessionId) {
        InterviewSessionDTO session = sessionService.getSession(sessionId);
        return Result.success(session);
    }
    
    /**
     * 获取当前问题
     */
    @GetMapping("/api/interview/sessions/{sessionId}/question")
    public Result<Map<String, Object>> getCurrentQuestion(@PathVariable String sessionId) {
        return Result.success(sessionService.getCurrentQuestionResponse(sessionId));
    }
    
    /**
     * 提交答案
     */
    @PostMapping("/api/interview/sessions/{sessionId}/answers")
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 10)
    public Result<SubmitAnswerResponse> submitAnswer(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> body) {
        Integer questionIndex = (Integer) body.get("questionIndex");
        String answer = (String) body.get("answer");
        log.info("提交答案: 会话{}, 问题{}", sessionId, questionIndex);
        SubmitAnswerRequest request = new SubmitAnswerRequest(sessionId, questionIndex, answer);
        SubmitAnswerResponse response = sessionService.submitAnswer(request);
        return Result.success(response);
    }
    
    /**
     * 生成面试报告
     */
    @GetMapping("/api/interview/sessions/{sessionId}/report")
    public Result<InterviewReportDTO> getReport(@PathVariable String sessionId) {
        log.info("生成面试报告: {}", sessionId);
        InterviewReportDTO report = sessionService.generateReport(sessionId);
        return Result.success(report);
    }
    
    /**
     * 查找未完成的面试会话
     * GET /api/interview/sessions/unfinished/{resumeId}
     */
    @GetMapping("/api/interview/sessions/unfinished/{resumeId}")
    public Result<InterviewSessionDTO> findUnfinishedSession(@PathVariable Long resumeId) {
        return Result.success(sessionService.findUnfinishedSessionOrThrow(resumeId));
    }
    
    /**
     * 暂存答案（不进入下一题）
     */
    @PutMapping("/api/interview/sessions/{sessionId}/answers")
    public Result<Void> saveAnswer(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> body) {
        Integer questionIndex = (Integer) body.get("questionIndex");
        String answer = (String) body.get("answer");
        log.info("暂存答案: 会话{}, 问题{}", sessionId, questionIndex);
        SubmitAnswerRequest request = new SubmitAnswerRequest(sessionId, questionIndex, answer);
        sessionService.saveAnswer(request);
        return Result.success(null);
    }
    
    /**
     * 提前交卷
     */
    @PostMapping("/api/interview/sessions/{sessionId}/complete")
    public Result<Void> completeInterview(@PathVariable String sessionId) {
        log.info("提前交卷: {}", sessionId);
        sessionService.completeInterview(sessionId);
        return Result.success(null);
    }
    
    /**
     * 获取面试会话详情
     * GET /api/interview/sessions/{sessionId}/details
     */
    @GetMapping("/api/interview/sessions/{sessionId}/details")
    public Result<InterviewDetailDTO> getInterviewDetail(@PathVariable String sessionId,
                                                         HttpServletRequest request) {
        AuthUser user = AuthSupport.requireAuth(request);
        InterviewDetailDTO detail = historyService.getInterviewDetail(sessionId, user.userId(),
            user.role() != Role.CANDIDATE);
        return Result.success(detail);
    }

    /**
     * 导出面试报告为PDF
     */
    @GetMapping("/api/interview/sessions/{sessionId}/export")
    public ResponseEntity<byte[]> exportInterviewPdf(@PathVariable String sessionId,
                                                     HttpServletRequest request) {
        AuthUser user = AuthSupport.requireAuth(request);
        try {
            byte[] pdfBytes = historyService.exportInterviewPdf(sessionId, user.userId(),
                user.role() != Role.CANDIDATE);
            String filename = URLEncoder.encode("模拟面试报告_" + sessionId + ".pdf", 
                StandardCharsets.UTF_8);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除面试会话
     */
    @DeleteMapping("/api/interview/sessions/{sessionId}")
    public Result<Void> deleteInterview(@PathVariable String sessionId, HttpServletRequest request) {
        AuthUser user = AuthSupport.requireAuth(request);
        persistenceService.findBySessionId(sessionId)
            .filter(s -> user.role() != Role.CANDIDATE
                || (s.getOwnerId() != null && s.getOwnerId().equals(user.userId())))
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "无权删除该面试会话"));
        log.info("删除面试会话: {}", sessionId);
        persistenceService.deleteSessionBySessionId(sessionId);
        return Result.success(null);
    }
}
