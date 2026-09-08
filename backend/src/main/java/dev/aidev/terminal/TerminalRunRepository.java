package dev.aidev.terminal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerminalRunRepository extends JpaRepository<TerminalRun, Long> {
    List<TerminalRun> findTop50ByProjectIdOrderByIdDesc(Long projectId);
}
