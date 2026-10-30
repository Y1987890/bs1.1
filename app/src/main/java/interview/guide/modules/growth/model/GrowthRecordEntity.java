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
 * 成长记录：由面试评价的薄弱项转化而来的复练任务，支撑"评价→差距→计划→复练"闭环。
 */
@Entity
@Getter
@Setter
@Table(name = "growth_records", indexes = {
    @Index(name = "idx_growth_owner", columnList = "ownerUserId")
})
public class GrowthRecordEntity {

  public enum Status { OPEN, DONE }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long ownerUserId;

  /** 薄弱维度（与评价类别对应） */
  @Column(nullable = false, length = 100)
  private String dimension;

  @Column(nullable = false, length = 500)
  private String issue;

  /** 练习任务/改进动作 */
  @Column(nullable = false, length = 1000)
  private String action;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.OPEN;

  /** 来源面试会话（可空） */
  @Column(length = 32)
  private String sourceSessionId;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  private LocalDateTime doneAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
  }
}