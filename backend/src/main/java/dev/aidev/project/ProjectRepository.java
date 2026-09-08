package dev.aidev.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.members m " +
           "WHERE (p.ownerId = :userId OR m.userId = :userId) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Project> findAccessible(@Param("userId") Long userId,
                                 @Param("status") String status,
                                 @Param("search") String search,
                                 Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.members m " +
           "WHERE (p.ownerId = :userId OR m.userId = :userId) " +
           "AND p.status = 'ACTIVE' ORDER BY p.lastActivityAt DESC")
    List<Project> findRecent(@Param("userId") Long userId, Pageable pageable);

    Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);
    long countByOwnerId(Long ownerId);
}
