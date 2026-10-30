package interview.guide.modules.growth.repository;

import interview.guide.modules.growth.model.GrowthRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrowthRecordRepository extends JpaRepository<GrowthRecordEntity, Long> {

  List<GrowthRecordEntity> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

  long countByOwnerUserIdAndStatus(Long ownerUserId, GrowthRecordEntity.Status status);
}