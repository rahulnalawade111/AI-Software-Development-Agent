package dev.aidev.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    List<AiConversation> findByProjectIdOrderByUpdatedAtDesc(Long projectId);
}
