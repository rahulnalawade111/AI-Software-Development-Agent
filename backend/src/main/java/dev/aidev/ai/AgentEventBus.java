package dev.aidev.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory event bus routing agent events (thought streams, tool calls,
 * action status changes, build progress) to SSE subscribers per conversation.
 */
@Service
public class AgentEventBus {

    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long conversationId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        List<SseEmitter> list = subscribers.computeIfAbsent(conversationId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        emitter.onCompletion(() -> list.remove(emitter));
        emitter.onTimeout(() -> list.remove(emitter));
        emitter.onError(t -> list.remove(emitter));
        return emitter;
    }

    public void publish(Long conversationId, String type, Object data) {
        List<SseEmitter> list = subscribers.get(conversationId);
        if (list == null || list.isEmpty()) return;
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("type", type);
        payload.put("data", data);
        payload.put("ts", System.currentTimeMillis());
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name(type)
                        .data(mapper.writeValueAsString(payload), org.springframework.http.MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                list.remove(emitter);
            }
        }
    }

    public void publish(Long conversationId, Map<String, Object> event) {
        String type = String.valueOf(event.getOrDefault("type", "event"));
        publish(conversationId, type, event.get("data"));
    }

    /** Close and drop all subscribers for a conversation (agent finished). */
    public void complete(Long conversationId) {
        List<SseEmitter> list = subscribers.remove(conversationId);
        if (list == null) return;
        for (SseEmitter emitter : list) {
            try { emitter.complete(); } catch (Exception ignored) {}
        }
    }
}
