package interview.guide.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 */
@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64)
  private String actorUsername;

  @Column(nullable = false, length = 64)
  private String action;

  @Column(nullable = false, length = 64)
  private String targetType;

  @Column(length = 64)
  private String targetId;

  @Column(length = 500)
  private String detail;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}