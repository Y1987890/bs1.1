package interview.guide.modules.growth.service;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.growth.dto.GrowthDtos.GrowthRecordRequest;
import interview.guide.modules.growth.dto.GrowthDtos.GrowthRecordView;
import interview.guide.modules.growth.dto.GrowthDtos.TrainingPlanRequest;
import interview.guide.modules.growth.dto.GrowthDtos.TrainingPlanView;
import interview.guide.modules.growth.dto.GrowthDtos.TrendPoint;
import interview.guide.modules.growth.model.GrowthRecordEntity;
import interview.guide.modules.growth.model.GrowthRecordEntity.Status;
import interview.guide.modules.growth.model.PositionEntity;
import interview.guide.modules.growth.model.TrainingPlanEntity;
import interview.guide.modules.growth.repository.GrowthRecordRepository;
import interview.guide.modules.growth.repository.PositionRepository;
import interview.guide.modules.growth.repository.TrainingPlanRepository;
import interview.guide.modules.interview.model.InterviewSessionEntity;
import interview.guide.modules.interview.repository.InterviewSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 训练计划与成长记录服务：支撑"薄弱项 → 练习任务 → 复练跟踪 → 成长趋势"闭环。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingGrowthService {

  private final TrainingPlanRepository trainingPlanRepository;
  private final GrowthRecordRepository growthRecordRepository;
  private final PositionRepository positionRepository;
  private final InterviewSessionRepository interviewSessionRepository;

  // ==================== 训练计划 ====================

  public List<TrainingPlanView> listPlans(Long ownerUserId) {
    return trainingPlanRepository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId).stream()
        .map(this::toPlanView)
        .toList();
  }

  public TrainingPlanView createPlan(Long ownerUserId, TrainingPlanRequest request) {
    if (request.positionId() != null) {
      PositionEntity position = positionRepository.findById(request.positionId())
          .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));
      log.debug("训练计划关联岗位: {}", position.getTitle());
    }
    TrainingPlanEntity entity = new TrainingPlanEntity();
    entity.setOwnerUserId(ownerUserId);
    entity.setPositionId(request.positionId());
    String title = (request.title() == null || request.title().isBlank())
        ? "综合训练计划" : request.title().trim();
    entity.setTitle(title);
    entity.setGoal(request.goal());
    entity.setStatus(TrainingPlanEntity.Status.ACTIVE);
    trainingPlanRepository.save(entity);
    log.info("创建训练计划: id={}, owner={}", entity.getId(), ownerUserId);
    return toPlanView(entity);
  }

  public TrainingPlanView completePlan(Long planId, Long ownerUserId, boolean isAdmin) {
    TrainingPlanEntity entity = trainingPlanRepository.findById(planId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "训练计划不存在"));
    if (!isAdmin && !entity.getOwnerUserId().equals(ownerUserId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "只能操作自己的训练计划");
    }
    entity.setStatus(TrainingPlanEntity.Status.COMPLETED);
    entity.setCompletedAt(LocalDateTime.now());
    trainingPlanRepository.save(entity);
    return toPlanView(entity);
  }

  // ==================== 成长记录 ====================

  public List<GrowthRecordView> listRecords(Long ownerUserId) {
    return growthRecordRepository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId).stream()
        .map(this::toRecordView)
        .toList();
  }

  public GrowthRecordView addRecord(Long ownerUserId, GrowthRecordRequest request) {
    GrowthRecordEntity entity = new GrowthRecordEntity();
    entity.setOwnerUserId(ownerUserId);
    entity.setDimension(request.dimension().trim());
    entity.setIssue(request.issue().trim());
    entity.setAction(request.action().trim());
    entity.setSourceSessionId(request.sourceSessionId());
    entity.setStatus(Status.OPEN);
    growthRecordRepository.save(entity);
    log.info("新增成长记录: id={}, owner={}, dimension={}", entity.getId(), ownerUserId, entity.getDimension());
    return toRecordView(entity);
  }

  public GrowthRecordView doneRecord(Long recordId, Long ownerUserId, boolean isAdmin) {
    GrowthRecordEntity entity = growthRecordRepository.findById(recordId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "成长记录不存在"));
    if (!isAdmin && !entity.getOwnerUserId().equals(ownerUserId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "只能操作自己的成长记录");
    }
    entity.setStatus(Status.DONE);
    entity.setDoneAt(LocalDateTime.now());
    growthRecordRepository.save(entity);
    return toRecordView(entity);
  }

  // ==================== 成长趋势 ====================

  /**
   * 最近 10 次已完成面试得分（旧数据 ownerId 为空时不计入 0 分之外的点）。
   */
  public List<TrendPoint> recentTrend(Long ownerUserId) {
    List<InterviewSessionEntity> sessions =
        interviewSessionRepository.findTop10ByOwnerIdAndOverallScoreIsNotNullOrderByCreatedAtDesc(ownerUserId);
    return sessions.stream()
        .map(s -> new TrendPoint(s.getSessionId(), s.getOverallScore(), s.getCreatedAt()))
        .toList();
  }

  private TrainingPlanView toPlanView(TrainingPlanEntity entity) {
    return new TrainingPlanView(
        entity.getId(), entity.getOwnerUserId(), entity.getPositionId(),
        entity.getTitle(), entity.getGoal(), entity.getStatus().name(),
        entity.getCreatedAt(), entity.getCompletedAt()
    );
  }

  private GrowthRecordView toRecordView(GrowthRecordEntity entity) {
    return new GrowthRecordView(
        entity.getId(), entity.getOwnerUserId(), entity.getDimension(),
        entity.getIssue(), entity.getAction(), entity.getStatus().name(),
        entity.getSourceSessionId(), entity.getCreatedAt(), entity.getDoneAt()
    );
  }
}