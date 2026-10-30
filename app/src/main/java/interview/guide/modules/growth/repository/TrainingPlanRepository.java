package interview.guide.modules.growth.repository;

import interview.guide.modules.growth.model.TrainingPlanEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlanEntity, Long> {

  List<TrainingPlanEntity> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
}