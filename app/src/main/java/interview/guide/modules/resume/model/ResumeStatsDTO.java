package interview.guide.modules.resume.model;

/**
 * 简历统计 DTO（简历历史页顶部指标卡）
 *
 * @param totalCount           可见简历总数
 * @param totalInterviewCount  关联面试总次数
 * @param totalAccessCount     简历累计被访问次数
 */
public record ResumeStatsDTO(long totalCount, long totalInterviewCount, long totalAccessCount) {
}
