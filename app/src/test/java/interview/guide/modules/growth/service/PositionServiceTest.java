package interview.guide.modules.growth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import interview.guide.auth.AuditLogService;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.growth.dto.GrowthDtos.PositionRequest;
import interview.guide.modules.growth.dto.GrowthDtos.RequirementItem;
import interview.guide.modules.growth.model.PositionEntity;
import interview.guide.modules.growth.model.PositionEntity.Status;
import interview.guide.modules.growth.repository.PositionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * 岗位服务权限与序列化单元测试。
 */
@DisplayName("岗位服务")
@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

  @Mock
  private PositionRepository positionRepository;

  @Mock
  private AuditLogService auditLogService;

  private PositionService service;

  @BeforeEach
  void setup() {
    service = new PositionService(positionRepository, new ObjectMapper(), auditLogService);
  }

  private PositionRequest request() {
    return new PositionRequest("Java 后端（校招）", "某司", "岗位描述",
        List.of(new RequirementItem("Java基础", 30, "JVM/并发")));
  }

  private PositionEntity entity(Long id, Long owner, Status status) {
    PositionEntity e = new PositionEntity();
    e.setId(id);
    e.setTitle("t");
    e.setCompany("c");
    e.setOwnerId(owner);
    e.setStatus(status);
    e.setRequirementsJson("[{\"dimension\":\"Java基础\",\"weight\":30,\"description\":\"JVM/并发\"}]");
    return e;
  }

  @Test
  @DisplayName("非创建者且非管理员无法发布岗位")
  void publishByOtherInterviewerForbidden() {
    PositionEntity e = entity(1L, 100L, Status.DRAFT);
    when(positionRepository.findById(1L)).thenReturn(Optional.of(e));

    assertThatThrownBy(() -> service.publish(1L, 200L, false, "itv2"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("只能发布自己创建的岗位");
  }

  @Test
  @DisplayName("创建者可以发布，并写入审计日志")
  void publishByOwner() {
    PositionEntity e = entity(1L, 100L, Status.DRAFT);
    when(positionRepository.findById(1L)).thenReturn(Optional.of(e));
    when(positionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    var view = service.publish(1L, 100L, false, "itv1");

    assertThat(view.status()).isEqualTo("PUBLISHED");
    verify(auditLogService).record("itv1", "PUBLISH_POSITION", "position", "1", "t");
  }

  @Test
  @DisplayName("创建岗位序列化能力要求并可回读")
  void createThenParseRequirements() {
    when(positionRepository.save(any())).thenAnswer(i -> {
      PositionEntity saved = i.getArgument(0);
      saved.setId(9L);
      return saved;
    });

    var view = service.create(request(), 100L, "itv1");

    assertThat(view.requirements()).hasSize(1);
    assertThat(view.requirements().get(0).dimension()).isEqualTo("Java基础");
    verify(auditLogService).record(eq("itv1"), eq("CREATE_POSITION"), eq("position"), eq("9"), any());
  }
}