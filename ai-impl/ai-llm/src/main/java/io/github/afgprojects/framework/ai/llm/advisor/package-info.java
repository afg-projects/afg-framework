/**
 * Advisor adapters for managing LLM request/response processing.
 * <p>
 * This package provides advisor implementations that integrate with the framework's
 * LLM clients to provide cross-cutting concerns like:
 * <ul>
 *   <li>System prompt management - {@link SystemPromptAdvisorAdapter}</li>
 *   <li>Conversation memory - {@link ChatMemoryAdvisorAdapter}</li>
 *   <li>RAG context retrieval - {@link RagAdvisorAdapter}</li>
 * </ul>
 *
 * <h2>Advisor Execution Order</h2>
 * <p>
 * Advisors are executed in order based on their {@link LlmAdvisor#getOrder()} value:
 * <ul>
 *   <li>Order 0-99: System prompt advisors</li>
 *   <li>Order 100-199: Memory advisors</li>
 *   <li>Order 200-299: RAG advisors</li>
 *   <li>Order 1000+: Logging and monitoring advisors</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create advisors
 * List<LlmAdvisor> advisors = List.of(
 *     new SystemPromptAdvisorAdapter("You are a helpful assistant."),
 *     new ChatMemoryAdvisorAdapter(memory, "session-1"),
 *     new RagAdvisorAdapter(retriever, 5)
 * );
 *
 * // Create client with advisors
 * OpenAiLlmClient client = new OpenAiLlmClient(config, toolRegistry, advisors);
 *
 * // Or add advisors to existing client
 * OpenAiLlmClient clientWithAdvisor = client.withAdvisor(new LoggingAdvisor());
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 * @see LlmAdvisor
 * @see SystemPromptAdvisorAdapter
 * @see ChatMemoryAdvisorAdapter
 * @see RagAdvisorAdapter
 */
package io.github.afgprojects.framework.ai.llm.advisor;
