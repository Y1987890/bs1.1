package interview.guide.modules.growth.service;

import interview.guide.auth.AuditLogService;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.growth.dto.GrowthDtos.PositionRequest;
import interview.guide.modules.growth.dto.GrowthDtos.PositionView;
import interview.guide.modules.growth.dto.GrowthDtos.RequirementItem;
import interview.guide.modules.growth.model.PositionEntity;
import interview.guide.modules.growth.model.PositionEntity.Status;
import interview.guide.modules.growth.repository.PositionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 岗位服务：岗位发布、能力要求管理（要求存 JSON 文本）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionService {

  private final PositionRepository positionRepository;
  private final ObjectMapper objectMapper;
  private final AuditLogService auditLogService;

  /**
   * 候选人视角：仅已发布岗位。
   */
  public List<PositionView> listPublished() {
    return positionRepository.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED).stream()
        .map(this::toView)
        .toList();
  }

  /**
   * 面试官视角：自己创建的（不限状态）+ 已发布岗位。
   */
  public List<PositionView> listForInterviewer(Long userId) {
    List<PositionEntity> owned = positionRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
    List<PositionEntity> published = positionRepository.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED);
    return java.util.stream.Stream.concat(owned.stream(), published.stream())
        .filter(p -> p.getStatus() != Status.ARCHIVED)
        .distinct()
        .map(this::toView)
        .toList();
  }

  /**
   * 管理员视角：全部岗位。
   */
  public List<PositionView> listAll() {
    return positionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
        .map(this::toView)
        .toList();
  }

  public PositionView getViewable(Long id, Long userId, boolean privileged) {
    PositionEntity entity = positionRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));
    if (entity.getStatus() == Status.PUBLISHED || privileged || entity.getOwnerId().equals(userId)) {
      return toView(entity);
    }
    throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该岗位");
  }

  public PositionView create(PositionRequest request, Long ownerId, String actorName) {
    PositionEntity entity = new PositionEntity();
    entity.setTitle(request.title().trim());
    entity.setCompany(request.company().trim());
    entity.setDescription(request.description());
    entity.setRequirementsJson(serializeRequirements(request.requirements()));
    entity.setStatus(Status.DRAFT);
    entity.setOwnerId(ownerId);
    positionRepository.save(entity);
    auditLogService.record(actorName, "CREATE_POSITION", "position", String.valueOf(entity.getId()), entity.getTitle());
    log.info("创建岗位: id={}, title={}, owner={}", entity.getId(), entity.getTitle(), ownerId);
    return toView(entity);
  }

  public PositionView update(Long id, PositionRequest request, Long userId, boolean isAdmin, String actorName) {
    PositionEntity entity = positionRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));
    if (!isAdmin && !entity.getOwnerId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "只能修改自己创建的岗位");
    }
    entity.setTitle(request.title().trim());
    entity.setCompany(request.company().trim());
    entity.setDescription(request.description());
    entity.setRequirementsJson(serializeRequirements(request.requirements()));
    positionRepository.save(entity);
    auditLogService.record(actorName, "UPDATE_POSITION", "position", String.valueOf(id), entity.getTitle());
    return toView(entity);
  }

  public PositionView publish(Long id, Long userId, boolean isAdmin, String actorName) {
    PositionEntity entity = positionRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));
    if (!isAdmin && !entity.getOwnerId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "只能发布自己创建的岗位");
    }
    entity.setStatus(Status.PUBLISHED);
    positionRepository.save(entity);
    auditLogService.record(actorName, "PUBLISH_POSITION", "position", String.valueOf(id), entity.getTitle());
    return toView(entity);
  }

  public PositionView archive(Long id, String actorName) {
    PositionEntity entity = positionRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));
    entity.setStatus(Status.ARCHIVED);
    positionRepository.save(entity);
    auditLogService.record(actorName, "ARCHIVE_POSITION", "position", String.valueOf(id), entity.getTitle());
    return toView(entity);
  }

  public void delete(Long id, String actorName) {
    PositionEntity entity = positionRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));
    positionRepository.delete(entity);
    auditLogService.record(actorName, "DELETE_POSITION", "position", String.valueOf(id), entity.getTitle());
  }

  private PositionView toView(PositionEntity entity) {
    return new PositionView(
        entity.getId(), entity.getTitle(), entity.getCompany(), entity.getDescription(),
        parseRequirements(entity.getRequirementsJson()), entity.getStatus().name(),
        entity.getOwnerId(), entity.getCreatedAt()
    );
  }

  private String serializeRequirements(List<RequirementItem> requirements) {
    try {
      return objectMapper.writeValueAsString(requirements);
    } catch (JacksonException e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "岗位能力要求序列化失败");
    }
  }

  private List<RequirementItem> parseRequirements(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (JacksonException e) {
      log.warn("岗位能力要求解析失败: {}", json);
      return List.of();
    }
  }
}