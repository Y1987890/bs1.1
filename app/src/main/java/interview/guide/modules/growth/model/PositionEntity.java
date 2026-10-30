package interview.guide.modules.growth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 岗位（Position）：职跃核心业务对象，携带能力要求（JSON 文本由 Service 序列化）。
 */
@Entity
@Getter
@Setter
@Table(name = "job_positions", indexes = {
    @Index(name = "idx_position_owner", columnList = "ownerId"),
    @Index(name = "idx_position_status", columnList = "status")
})
public class PositionEntity {

  public enum Status { DRAFT, PUBLISHED, ARCHIVED }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, length = 200)
  private String company;

  @Column(columnDefinition = "TEXT")
  private String description;

  /** 能力要求 JSON：[{dimension, weight, description}] */
  @Column(columnDefinition = "TEXT")
  private String requirementsJson;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.DRAFT;

  /** 创建者用户 id（INTERVIEWER 或 ADMIN） */
  private Long ownerId;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
  }
}