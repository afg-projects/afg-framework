package io.github.afgprojects.framework.ai.agentic;

import io.github.afgprojects.framework.ai.core.agent.Agent;
import io.github.afgprojects.framework.ai.core.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * AFG Agent 与 LangChain4j 的适配器
 *
 * <p>将 AFG Agent 接口适配到 LangChain4j 的 AiServices 模型。
 * 提供两种使用方式：
 * <ul>
 *   <li>AFG Agent 风格 - 实现 {@link Agent} 接口</li>
 *   <li>LangChain4j 风格 - 使用 AiServices Builder</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 创建 AFG Agent 适配器
 * AfgAgent agent = AfgAgent.builder()
 *     .name("MyAgent")
 *     .description("A helpful assistant")
 *     .delegate(delegate)
 *     .build();
 *
 * // 执行任务
 * AgentResponse response = agent.execute(new AgentRequest("session-1", "Hello"));
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class AfgAgent implements Agent {

    private final String name;
    private final String description;
    private final List<Tool<?, ?>> tools;
    private final LangChain4jDelegate delegate;

    /**
     * 私有构造函数，使用 Builder 创建实例
     */
    private AfgAgent(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.tools = List.copyOf(builder.tools);
        this.delegate = builder.delegate;
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public String getDescription() {
        return description;
    }

    @Override
    @NonNull
    public AgentResponse execute(@NonNull AgentRequest request) {
        try {
            String result = delegate.execute(request.userInput());
            return AgentResponse.completed(result);
        } catch (Exception e) {
            return AgentResponse.error(e.getMessage(), e);
        }
    }

    @Override
    @NonNull
    public List<Tool<?, ?>> getTools() {
        return tools;
    }

    /**
     * LangChain4j 委托接口
     *
     * <p>由 LangChain4j AiServices 生成的实现类实现此接口。
     */
    public interface LangChain4jDelegate {

        /**
         * 执行任务
         *
         * @param input 用户输入
         * @return 执行结果
         */
        @NonNull
        String execute(@NonNull String input);
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * AfgAgent 构建器
     */
    public static class Builder {

        private String name = "AfgAgent";
        private String description = "AFG Agent";
        private List<Tool<?, ?>> tools = List.of();
        private LangChain4jDelegate delegate;

        /**
         * 设置 Agent 名称
         */
        public Builder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置 Agent 描述
         */
        public Builder description(@NonNull String description) {
            this.description = description;
            return this;
        }

        /**
         * 设置工具列表
         */
        public Builder tools(@NonNull List<Tool<?, ?>> tools) {
            this.tools = tools;
            return this;
        }

        /**
         * 设置 LangChain4j 委托
         */
        public Builder delegate(@NonNull LangChain4jDelegate delegate) {
            this.delegate = delegate;
            return this;
        }

        /**
         * 构建 AfgAgent 实例
         */
        public AfgAgent build() {
            if (delegate == null) {
                throw new IllegalStateException("Delegate is required");
            }
            return new AfgAgent(this);
        }
    }
}