package dev.aidev.build;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildRunRepository extends JpaRepository<BuildRun, Long> {
    List<BuildRun> findTop20ByProjectIdOrderByIdDesc(Long projectId);
}
