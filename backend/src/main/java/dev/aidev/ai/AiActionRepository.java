package dev.aidev.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiActionRepository extends JpaRepository<AiAction, Long> {
    List<AiAction> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);
    List<AiAction> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
