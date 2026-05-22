package io.github.afgprojects.framework.ai.agentic;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import io.github.afgprojects.framework.ai.core.agent.Agent;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * AFG Agent Builder
 *
 * <p>基于 LangChain4j AiServices 构建符合 AFG Agent 接口的智能体。
 *
 * <p>使用示例：
 * <pre>{@code
 * Agent agent = AfgAgentBuilder.create()
 *     .name("WeatherAgent")
 *     .description("Weather forecast assistant")
 *     .chatModel(chatModel)
 *     .systemMessage("You are a weather assistant.")
 *     .tools(List.of(new WeatherTool()))
 *     .maxMemoryMessages(10)
 *     .build();
 *
 * AgentResponse response = agent.execute(new AgentRequest("session-1", "What's the weather?"));
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class AfgAgentBuilder {

    private String name = "AfgAgent";
    private String description = "AFG Agent";
    private ChatModel chatModel;
    private String systemMessage;
    private final List<Tool<?, ?>> tools = new ArrayList<>();
    private int maxMemoryMessages = 10;

    private AfgAgentBuilder() {
    }

    /**
     * 创建 Builder
     */
    public static AfgAgentBuilder create() {
        return new AfgAgentBuilder();
    }

    /**
     * 设置 Agent 名称
     */
    public AfgAgentBuilder name(@NonNull String name) {
        this.name = name;
        return this;
    }

    /**
     * 设置 Agent 描述
     */
    public AfgAgentBuilder description(@NonNull String description) {
        this.description = description;
        return this;
    }

    /**
     * 设置 ChatModel（LangChain4j 1.0.0 API）
     */
    public AfgAgentBuilder chatModel(@NonNull ChatModel chatModel) {
        this.chatModel = chatModel;
        return this;
    }

    /**
     * 设置系统消息
     */
    public AfgAgentBuilder systemMessage(@Nullable String systemMessage) {
        this.systemMessage = systemMessage;
        return this;
    }

    /**
     * 添加工具
     */
    public AfgAgentBuilder tool(@NonNull Tool<?, ?> tool) {
        this.tools.add(tool);
        return this;
    }

    /**
     * 设置工具列表
     */
    public AfgAgentBuilder tools(@NonNull List<Tool<?, ?>> tools) {
        this.tools.clear();
        this.tools.addAll(tools);
        return this;
    }

    /**
     * 设置最大记忆消息数
     */
    public AfgAgentBuilder maxMemoryMessages(int maxMemoryMessages) {
        this.maxMemoryMessages = maxMemoryMessages;
        return this;
    }

    /**
     * 构建 Agent
     */
    public Agent build() {
        if (chatModel == null) {
            throw new IllegalStateException("ChatModel is required");
        }

        // 创建 ChatMemory
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(maxMemoryMessages);

        // 构建 AiServices
        AiServices<AfgAgent.LangChain4jDelegate> aiServicesBuilder = AiServices.builder(AfgAgent.LangChain4jDelegate.class)
            .chatModel(chatModel)
            .chatMemory(chatMemory);

        // 设置系统消息
        if (systemMessage != null && !systemMessage.isBlank()) {
            aiServicesBuilder.systemMessageProvider((id) -> systemMessage);
        }

        // 添加工具（使用 ToolProvider 方式）
        if (!tools.isEmpty()) {
            aiServicesBuilder.tools(tools.stream()
                .map(ToolAdapter::toToolSpecification)
                .toList());
        }

        AfgAgent.LangChain4jDelegate delegate = aiServicesBuilder.build();

        return AfgAgent.builder()
            .name(name)
            .description(description)
            .tools(tools)
            .delegate(delegate)
            .build();
    }
}