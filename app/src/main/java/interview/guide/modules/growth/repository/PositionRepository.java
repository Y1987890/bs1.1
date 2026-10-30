package interview.guide.modules.growth.repository;

import interview.guide.modules.growth.model.PositionEntity;
import interview.guide.modules.growth.model.PositionEntity.Status;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<PositionEntity, Long> {

  List<PositionEntity> findByStatusOrderByCreatedAtDesc(Status status);

  List<PositionEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}