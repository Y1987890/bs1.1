package interview.guide.modules.growth.controller;

import interview.guide.auth.AuthSupport;
import interview.guide.auth.AuthUser;
import interview.guide.auth.Role;
import interview.guide.common.result.Result;
import interview.guide.modules.growth.dto.GrowthDtos.GrowthRecordRequest;
import interview.guide.modules.growth.dto.GrowthDtos.GrowthRecordView;
import interview.guide.modules.growth.dto.GrowthDtos.TrendPoint;
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
 * 成长记录与趋势接口（职跃）：薄弱项 → 练习任务 → 复练 → 趋势。
 */
@Tag(name = "成长档案", description = "成长记录与历史趋势（职跃）")
@RestController
@RequestMapping("/api/v1/growth")
@RequiredArgsConstructor
public class GrowthController {

  private final TrainingGrowthService trainingGrowthService;

  @GetMapping("/records")
  public Result<List<GrowthRecordView>> listRecords(HttpServletRequest request) {
    AuthUser user = AuthSupport.requireAuth(request);
    return Result.success(trainingGrowthService.listRecords(user.userId()));
  }

  @PostMapping("/records")
  public Result<GrowthRecordView> addRecord(@Valid @RequestBody GrowthRecordRequest body,
                                            HttpServletRequest request) {
    AuthUser user = AuthSupport.requireAnyRole(request, Role.CANDIDATE);
    return Result.success(trainingGrowthService.addRecord(user.userId(), body));
  }

  @PostMapping("/records/{id}/done")
  public Result<GrowthRecordView> doneRecord(@PathVariable Long id, HttpServletRequest request) {
    AuthUser user = AuthSupport.requireAnyRole(request, Role.CANDIDATE, Role.ADMIN);
    return Result.success(trainingGrowthService.doneRecord(id, user.userId(),
        user.role() == Role.ADMIN));
  }

  @GetMapping("/trend")
  public Result<List<TrendPoint>> trend(HttpServletRequest request) {
    AuthUser user = AuthSupport.requireAuth(request);
    return Result.success(trainingGrowthService.recentTrend(user.userId()));
  }
}