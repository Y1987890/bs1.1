package interview.guide.modules.growth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.growth.dto.GrowthDtos.GrowthRecordRequest;
import interview.guide.modules.growth.dto.GrowthDtos.TrainingPlanRequest;
import interview.guide.modules.growth.model.GrowthRecordEntity;
import interview.guide.modules.growth.model.TrainingPlanEntity;
import interview.guide.modules.growth.repository.GrowthRecordRepository;
import interview.guide.modules.growth.repository.PositionRepository;
import interview.guide.modules.growth.repository.TrainingPlanRepository;
import interview.guide.modules.interview.model.InterviewSessionEntity;
import interview.guide.modules.interview.repository.InterviewSessionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 训练计划与成长记录服务单元测试（含归属校验）。
 */
@DisplayName("训练与成长服务")
@ExtendWith(MockitoExtension.class)
class TrainingGrowthServiceTest {

  @Mock
  private TrainingPlanRepository planRepository;

  @Mock
  private GrowthRecordRepository recordRepository;

  @Mock
  private PositionRepository positionRepository;

  @Mock
  private InterviewSessionRepository sessionRepository;

  private TrainingGrowthService service;

  @BeforeEach
  void setup() {
    service = new TrainingGrowthService(planRepository, recordRepository, positionRepository,
        sessionRepository);
  }

  @Test
  @DisplayName("创建计划时关联的岗位必须存在")
  void createPlanRequiresPosition() {
    when(positionRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.createPlan(1L, new TrainingPlanRequest("t", "g", 99L)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getCode())
        .isEqualTo(ErrorCode.POSITION_NOT_FOUND.getCode());
  }

  @Test
  @DisplayName("候选人不能完成他人的成长记录")
  void doneRecordOwnershipForbidden() {
    GrowthRecordEntity e = new GrowthRecordEntity();
    e.setId(1L);
    e.setOwnerUserId(100L);
    when(recordRepository.findById(1L)).thenReturn(Optional.of(e));

    assertThatThrownBy(() -> service.doneRecord(1L, 200L, false))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("只能操作自己的成长记录");
    verify(recordRepository, never()).save(any());
  }

  @Test
  @DisplayName("管理员可代为完成成长记录")
  void doneRecordByAdmin() {
    GrowthRecordEntity e = new GrowthRecordEntity();
    e.setId(1L);
    e.setOwnerUserId(100L);
    when(recordRepository.findById(1L)).thenReturn(Optional.of(e));
    when(recordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    var view = service.doneRecord(1L, 1L, true);

    assertThat(view.status()).isEqualTo("DONE");
  }

  @Test
  @DisplayName("成长趋势只统计本人最近得分会话")
  void recentTrendScopedToOwner() {
    InterviewSessionEntity s = new InterviewSessionEntity();
    s.setSessionId("s1");
    s.setOverallScore(77);
    when(sessionRepository.findTop10ByOwnerIdAndOverallScoreIsNotNullOrderByCreatedAtDesc(1L))
        .thenReturn(List.of(s));

    var points = service.recentTrend(1L);

    assertThat(points).hasSize(1);
    assertThat(points.get(0).overallScore()).isEqualTo(77);
    verify(sessionRepository).findTop10ByOwnerIdAndOverallScoreIsNotNullOrderByCreatedAtDesc(1L);
  }

  @Test
  @DisplayName("新增成长记录默认 OPEN 且归属创建人")
  void addRecordDefaults() {
    when(recordRepository.save(any())).thenAnswer(i -> {
      GrowthRecordEntity e = i.getArgument(0);
      e.setId(5L);
      return e;
    });

    var view = service.addRecord(7L,
        new GrowthRecordRequest("Redis", "缓存穿透理解不足", "完成 3 道场景题", "s9"));

    assertThat(view.status()).isEqualTo("OPEN");
    assertThat(view.ownerUserId()).isEqualTo(7L);
    assertThat(view.sourceSessionId()).isEqualTo("s9");
  }
}