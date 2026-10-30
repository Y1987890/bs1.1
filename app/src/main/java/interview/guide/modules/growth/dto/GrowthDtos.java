package interview.guide.modules.growth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public class GrowthDtos {

  /** 岗位能力要求单项 */
  public record RequirementItem(
      @NotBlank(message = "维度名称不能为空") @Size(max = 50) String dimension,
      @NotNull @Min(1) @Max(100) Integer weight,
      @Size(max = 500) String description
  ) {}

  public record PositionRequest(
      @NotBlank(message = "岗位名称不能为空") @Size(max = 200) String title,
      @NotBlank(message = "公司名称不能为空") @Size(max = 200) String company,
      @Size(max = 5000) String description,
      @NotEmpty(message = "至少配置一项能力要求") List<@Valid RequirementItem> requirements
  ) {}

  public record PositionView(
      Long id,
      String title,
      String company,
      String description,
      List<RequirementItem> requirements,
      String status,
      Long ownerId,
      LocalDateTime createdAt
  ) {}

  public record TrainingPlanRequest(
      @Size(max = 200) String title,
      @Size(max = 1000) String goal,
      Long positionId
  ) {}

  public record TrainingPlanView(
      Long id,
      Long ownerUserId,
      Long positionId,
      String title,
      String goal,
      String status,
      LocalDateTime createdAt,
      LocalDateTime completedAt
  ) {}

  public record GrowthRecordRequest(
      @NotBlank(message = "薄弱维度不能为空") @Size(max = 100) String dimension,
      @NotBlank(message = "问题描述不能为空") @Size(max = 500) String issue,
      @NotBlank(message = "练习任务不能为空") @Size(max = 1000) String action,
      @Size(max = 32) String sourceSessionId
  ) {}

  public record GrowthRecordView(
      Long id,
      Long ownerUserId,
      String dimension,
      String issue,
      String action,
      String status,
      String sourceSessionId,
      LocalDateTime createdAt,
      LocalDateTime doneAt
  ) {}

  /** 历史成长趋势点 */
  public record TrendPoint(
      String sessionId,
      Integer overallScore,
      LocalDateTime createdAt
  ) {}

  private GrowthDtos() {}
}