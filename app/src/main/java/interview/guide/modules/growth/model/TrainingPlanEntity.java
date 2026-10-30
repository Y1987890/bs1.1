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
 * 候选人训练计划：围绕目标岗位或薄弱项制定的复练计划。
 */
@Entity
@Getter
@Setter
@Table(name = "training_plans", indexes = {
    @Index(name = "idx_plan_owner", columnList = "ownerUserId")
})
public class TrainingPlanEntity {

  public enum Status { ACTIVE, COMPLETED }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 计划归属候选人 */
  @Column(nullable = false)
  private Long ownerUserId;

  /** 关联岗位（可空：自由训练） */
  private Long positionId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(length = 1000)
  private String goal;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.ACTIVE;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  private LocalDateTime completedAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
  }
}