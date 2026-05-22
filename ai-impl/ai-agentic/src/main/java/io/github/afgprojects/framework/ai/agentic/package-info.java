/**
 * AFG Agentic 模块 - LangChain4j 适配层
 *
 * <p>本模块提供 AFG Agent 接口与 LangChain4j 框架的集成：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.ai.agentic.AfgAgent} - AFG Agent 适配器</li>
 *   <li>{@link io.github.afgprojects.framework.ai.agentic.AfgAgentBuilder} - Agent 构建器</li>
 *   <li>{@link io.github.afgprojects.framework.ai.agentic.ToolAdapter} - Tool 适配器</li>
 * </ul>
 *
 * <h2>快速开始</h2>
 * <pre>{@code
 * // 创建 Agent
 * Agent agent = AfgAgentBuilder.create()
 *     .name("MyAgent")
 *     .description("A helpful assistant")
 *     .chatLanguageModel(chatModel)
 *     .systemMessage("You are a helpful assistant.")
 *     .tools(List.of(new MyTool()))
 *     .build();
 *
 * // 执行任务
 * AgentResponse response = agent.execute(AgentRequest.of("Hello!"));
 * }</pre>
 *
 * <h2>与 AFG StateGraph 集成</h2>
 * <p>本模块与 {@code ai-impl:ai-agent} 中的 StateGraph/Node 体系完全独立，
 * 可以根据需要选择使用：
 * <ul>
 *   <li>简单 Agent - 使用本模块的 AfgAgent</li>
 *   <li>复杂工作流 - 使用 ai-agent 模块的 StateGraph</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
package io.github.afgprojects.framework.ai.agentic;