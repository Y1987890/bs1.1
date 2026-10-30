package interview.guide.modules.growth.controller;

import interview.guide.auth.AuthSupport;
import interview.guide.auth.AuthUser;
import interview.guide.auth.Role;
import interview.guide.common.result.Result;
import interview.guide.modules.growth.dto.GrowthDtos.PositionRequest;
import interview.guide.modules.growth.dto.GrowthDtos.PositionView;
import interview.guide.modules.growth.service.PositionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 岗位接口（职跃）：候选人浏览已发布岗位；面试官/管理员维护岗位与发布。
 */
@Tag(name = "岗位管理", description = "岗位浏览、创建、编辑与发布（职跃）")
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class PositionController {

  private final PositionService positionService;

  @GetMapping
  public Result<List<PositionView>> list(HttpServletRequest request) {
    AuthUser user = AuthSupport.requireAuth(request);
    if (user.role() == Role.ADMIN) {
      return Result.success(positionService.listAll());
    }
    if (user.role() == Role.INTERVIEWER) {
      return Result.success(positionService.listForInterviewer(user.userId()));
    }
    return Result.success(positionService.listPublished());
  }

  @GetMapping("/{id}")
  public Result<PositionView> get(@PathVariable Long id, HttpServletRequest request) {
    AuthUser user = AuthSupport.requireAuth(request);
    boolean privileged = user.role() == Role.ADMIN || user.role() == Role.INTERVIEWER;
    return Result.success(positionService.getViewable(id, user.userId(), privileged));
  }

  @PostMapping
  public Result<PositionView> create(@Valid @RequestBody PositionRequest request,
                                     HttpServletRequest httpRequest) {
    AuthUser user = AuthSupport.requireAnyRole(httpRequest, Role.INTERVIEWER, Role.ADMIN);
    return Result.success(positionService.create(request, user.userId(), user.username()));
  }

  @PutMapping("/{id}")
  public Result<PositionView> update(@PathVariable Long id,
                                     @Valid @RequestBody PositionRequest request,
                                     HttpServletRequest httpRequest) {
    AuthUser user = AuthSupport.requireAnyRole(httpRequest, Role.INTERVIEWER, Role.ADMIN);
    return Result.success(positionService.update(id, request, user.userId(),
        user.role() == Role.ADMIN, user.username()));
  }

  @PostMapping("/{id}/publish")
  public Result<PositionView> publish(@PathVariable Long id, HttpServletRequest httpRequest) {
    AuthUser user = AuthSupport.requireAnyRole(httpRequest, Role.INTERVIEWER, Role.ADMIN);
    return Result.success(positionService.publish(id, user.userId(),
        user.role() == Role.ADMIN, user.username()));
  }

  @PostMapping("/{id}/archive")
  public Result<PositionView> archive(@PathVariable Long id, HttpServletRequest httpRequest) {
    AuthUser user = AuthSupport.requireAnyRole(httpRequest, Role.ADMIN);
    return Result.success(positionService.archive(id, user.username()));
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
    AuthUser user = AuthSupport.requireAnyRole(httpRequest, Role.ADMIN);
    positionService.delete(id, user.username());
    return Result.success(null);
  }
}