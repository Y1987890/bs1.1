package interview.guide.modules.growth.controller;

import interview.guide.auth.AuthSupport;
import interview.guide.auth.AuthUser;
import interview.guide.auth.Role;
import interview.guide.common.result.Result;
import interview.guide.modules.growth.dto.GrowthDtos.TrainingPlanRequest;
import interview.guide.modules.growth.dto.GrowthDtos.TrainingPlanView;
import interview.guide.modules.growth.service.TrainingGrowthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 训练计划接口（职跃）：候选人围绕岗位/薄弱项制定复练计划。
 */
@Tag(name = "训练计划", description = "训练计划创建、完成（职跃）")
@RestController
@RequestMapping("/api/v1/training-plans")
@RequiredArgsConstructor
public class TrainingPlanController {

  private final TrainingGrowthService trainingGrowthService;

  @GetMapping
  public Result<List<TrainingPlanView>> list(HttpServletRequest request) {
    AuthUser user = AuthSupport.requireAuth(request);
    return Result.success(trainingGrowthService.listPlans(user.userId()));
  }

  @PostMapping
  public Result<TrainingPlanView> create(@Valid @RequestBody TrainingPlanRequest body,
                                         HttpServletRequest request) {
    AuthUser user = AuthSupport.requireAnyRole(request, Role.CANDIDATE);
    return Result.success(trainingGrowthService.createPlan(user.userId(), body));
  }

  @PostMapping("/{id}/complete")
  public Result<TrainingPlanView> complete(@PathVariable Long id, HttpServletRequest request) {
    AuthUser user = AuthSupport.requireAnyRole(request, Role.CANDIDATE, Role.ADMIN);
    return Result.success(trainingGrowthService.completePlan(id, user.userId(),
        user.role() == Role.ADMIN));
  }
}