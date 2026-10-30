package interview.guide.modules.resume.service;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.infrastructure.export.PdfExportService;
import interview.guide.infrastructure.mapper.InterviewMapper;
import interview.guide.infrastructure.mapper.ResumeMapper;
import interview.guide.modules.interview.model.InterviewHistoryItemDTO;
import interview.guide.modules.interview.model.ResumeAnalysisResponse;
import interview.guide.modules.interview.service.InterviewPersistenceService;
import interview.guide.modules.resume.model.ResumeAnalysisEntity;
import interview.guide.modules.resume.model.ResumeDetailDTO;
import interview.guide.modules.resume.model.ResumeEntity;
import interview.guide.modules.resume.model.ResumeListItemDTO;
import interview.guide.modules.resume.model.ResumeStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 简历历史服务
 * 简历历史和导出简历分析报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeHistoryService {

    private final ResumePersistenceService resumePersistenceService;
    private final InterviewPersistenceService interviewPersistenceService;
    private final PdfExportService pdfExportService;
    private final ObjectMapper objectMapper;
    private final ResumeMapper resumeMapper;
    private final InterviewMapper interviewMapper;

    /**
     * 获取所有简历列表（面试官/管理员视角）
     */
    public List<ResumeListItemDTO> getAllResumes() {
        List<ResumeEntity> resumes = resumePersistenceService.findAllResumes();
        return toListItemDTOs(resumes);
    }

    /**
     * 数据归属：获取本人简历列表（候选人视角，职跃 RBAC）
     */
    public List<ResumeListItemDTO> getResumesByOwner(Long ownerId) {
        List<ResumeEntity> resumes = resumePersistenceService.findByOwnerIdOrderByUploadedAtDesc(ownerId);
        return toListItemDTOs(resumes);
    }

    /**
     * 数据归属：简历统计（特权角色全局，候选人仅本人；职跃简历历史页指标卡）
     */
    public ResumeStatsDTO getStatistics(Long requesterId, boolean privileged) {
        return new ResumeStatsDTO(
            resumePersistenceService.countResumes(requesterId, privileged),
            interviewPersistenceService.countSessions(requesterId, privileged),
            resumePersistenceService.sumAccessCount(requesterId, privileged)
        );
    }

    private List<ResumeListItemDTO> toListItemDTOs(List<ResumeEntity> resumes) {
        return resumes.stream().map(resume -> {
            // 获取最新分析结果的分数
            Integer latestScore = null;
            LocalDateTime lastAnalyzedAt = null;
            Optional<ResumeAnalysisEntity> analysisOpt = resumePersistenceService.getLatestAnalysis(resume.getId());
            if (analysisOpt.isPresent()) {
                ResumeAnalysisEntity analysis = analysisOpt.get();
                latestScore = analysis.getOverallScore();
                lastAnalyzedAt = analysis.getAnalyzedAt();
            }

            // 获取面试次数
            int interviewCount = interviewPersistenceService.findByResumeId(resume.getId()).size();

            // 使用 MapStruct 映射
            return new ResumeListItemDTO(
                resume.getId(),
                resume.getOriginalFilename(),
                resume.getFileSize(),
                resume.getUploadedAt(),
                resume.getAccessCount(),
                latestScore,
                lastAnalyzedAt,
                interviewCount,
                resume.getAnalyzeStatus(),
                resume.getAnalyzeError()
            );
        }).toList();
    }

    /**
     * 获取简历详情（包含分析历史）
     */
    public ResumeDetailDTO getResumeDetail(Long id, Long requesterId, boolean privileged) {
        Optional<ResumeEntity> resumeOpt = resumePersistenceService.findById(id);
        if (resumeOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }

        ResumeEntity resume = resumeOpt.get();
        checkOwnership(resume, requesterId, privileged);

        // 获取所有分析记录，使用 MapStruct 批量转换
        List<ResumeAnalysisEntity> analyses = resumePersistenceService.findAnalysesByResumeId(id);
        List<ResumeDetailDTO.AnalysisHistoryDTO> analysisHistory = resumeMapper.toAnalysisHistoryDTOList(
            analyses,
            this::extractStrengths,
            this::extractSuggestions
        );

        // 使用 InterviewMapper 转换面试历史
        List<InterviewHistoryItemDTO> interviewHistory = interviewMapper.toInterviewHistoryList(
            interviewPersistenceService.findByResumeId(id)
        );

        return new ResumeDetailDTO(
            resume.getId(),
            resume.getOriginalFilename(),
            resume.getFileSize(),
            resume.getContentType(),
            resume.getStorageUrl(),
            resume.getUploadedAt(),
            resume.getAccessCount(),
            resume.getResumeText(),
            resume.getAnalyzeStatus(),
            resume.getAnalyzeError(),
            analysisHistory,
            interviewHistory
        );
    }

    /**
     * 从 JSON 提取 strengths
     */
    private List<String> extractStrengths(ResumeAnalysisEntity entity) {
        try {
            if (entity.getStrengthsJson() != null) {
                return objectMapper.readValue(
                    entity.getStrengthsJson(),
                        new TypeReference<>() {
                        }
                );
            }
        } catch (JacksonException e) {
            log.error("解析 strengths JSON 失败", e);
        }
        return List.of();
    }

    /**
     * 从 JSON 提取 suggestions
     */
    private List<Object> extractSuggestions(ResumeAnalysisEntity entity) {
        try {
            if (entity.getSuggestionsJson() != null) {
                return objectMapper.readValue(
                    entity.getSuggestionsJson(),
                        new TypeReference<>() {
                        }
                );
            }
        } catch (JacksonException e) {
            log.error("解析 suggestions JSON 失败", e);
        }
        return List.of();
    }

    /**
     * 导出简历分析报告为PDF
     */
    public ExportResult exportAnalysisPdf(Long resumeId, Long requesterId, boolean privileged) {
        Optional<ResumeEntity> resumeOpt = resumePersistenceService.findById(resumeId);
        if (resumeOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }

        ResumeEntity resume = resumeOpt.get();
        checkOwnership(resume, requesterId, privileged);
        Optional<ResumeAnalysisResponse> analysisOpt = resumePersistenceService.getLatestAnalysisAsDTO(resumeId);
        if (analysisOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_NOT_FOUND);
        }

        try {
            byte[] pdfBytes = pdfExportService.exportResumeAnalysis(resume, analysisOpt.get());
            String filename = "简历分析报告_" + resume.getOriginalFilename() + ".pdf";

            return new ExportResult(pdfBytes, filename);
        } catch (Exception e) {
            log.error("导出PDF失败: resumeId={}", resumeId, e);
            throw new BusinessException(ErrorCode.EXPORT_PDF_FAILED, "导出PDF失败: " + e.getMessage());
        }
    }

    /**
     * PDF导出结果
     */
    public record ExportResult(byte[] pdfBytes, String filename) {}

    /**
     * 数据归属校验：候选人只能访问自己的简历；历史无主数据仅特权角色可见。
     */
    private void checkOwnership(ResumeEntity resume, Long requesterId, boolean privileged) {
        if (!privileged && (resume.getOwnerId() == null || !resume.getOwnerId().equals(requesterId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该简历");
        }
    }
}

