package interview.guide.auth;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

  private static final int DETAIL_MAX_LENGTH = 500;

  private final AuditLogRepository auditLogRepository;

  public void record(String actor, String action, String targetType, String targetId, String detail) {
    AuditLogEntity log = new AuditLogEntity();
    log.setActorUsername(actor);
    log.setAction(action);
    log.setTargetType(targetType);
    log.setTargetId(targetId);
    log.setDetail(truncate(detail));
    auditLogRepository.save(log);
  }

  public List<AuditLogEntity> listRecent() {
    return auditLogRepository.findTop100ByOrderByIdDesc();
  }

  private String truncate(String detail) {
    if (detail == null || detail.length() <= DETAIL_MAX_LENGTH) {
      return detail;
    }
    return detail.substring(0, DETAIL_MAX_LENGTH);
  }
}