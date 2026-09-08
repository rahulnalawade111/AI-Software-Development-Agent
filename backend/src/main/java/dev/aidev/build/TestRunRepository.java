package dev.aidev.build;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestRunRepository extends JpaRepository<TestRun, Long> {
    List<TestRun> findTop20ByProjectIdOrderByIdDesc(Long projectId);
}
