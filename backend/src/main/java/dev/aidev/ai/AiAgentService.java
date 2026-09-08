package dev.aidev.ai;

import dev.aidev.ai.llm.LlmChatResult;
import dev.aidev.ai.llm.LlmClient;
import dev.aidev.ai.llm.LlmMessage;
import dev.aidev.ai.llm.LlmToolSpec;
import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.ai.tools.ToolRegistry;
import dev.aidev.project.Project;
import dev.aidev.project.ProjectRepository;
import dev.aidev.project.ProjectService;
import dev.aidev.security.AppPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    private static final int MAX_TURNS = 48;
    private static final long COMMAND_TIMEOUT_SECONDS = 300;
    private static final long LLM_TIMEOUT_SECONDS = 180;

    private final LlmClient llm;
    private final ToolRegistry toolRegistry;
    private final AgentEventBus eventBus;
    private final AiConversationRepository conversations;
    private final AiMessageRepository messages;
    private final AiActionRepository actions;
    per-field-placeholder
