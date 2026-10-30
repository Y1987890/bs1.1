package interview.guide.modules.resume.repository;

import interview.guide.modules.resume.model.ResumeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 简历Repository
 */
@Repository
public interface ResumeRepository extends JpaRepository<ResumeEntity, Long> {
    
    /**
     * 根据文件哈希查找简历（用于去重）
     */
    Optional<ResumeEntity> findByFileHash(String fileHash);
    
    /**
     * 检查文件哈希是否存在
     */
    boolean existsByFileHash(String fileHash);

    /**
     * 数据归属：按所有者查找简历（职跃 RBAC）
     */
    List<ResumeEntity> findByOwnerIdOrderByUploadedAtDesc(Long ownerId);

    /**
     * 数据归属：统计本人简历数（职跃简历统计）
     */
    long countByOwnerId(Long ownerId);

    /**
     * 数据归属：统计本人简历累计被访问次数
     */
    @Query("SELECT COALESCE(SUM(r.accessCount), 0) FROM ResumeEntity r WHERE r.ownerId = :ownerId")
    long sumAccessCountByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * 全部简历累计被访问次数（面试官/管理员视角）
     */
    @Query("SELECT COALESCE(SUM(r.accessCount), 0) FROM ResumeEntity r")
    long sumAccessCount();
}
