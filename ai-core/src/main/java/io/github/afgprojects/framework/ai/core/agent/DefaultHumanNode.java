package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanInteraction;
import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanDecision;
import io.github.afgprojects.framework.ai.core.api.multiagent.node.HumanNode;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 默认人工节点实现
 *
 * <p>提供人机交互的工作流节点，暂停工作流等待人工输入。
 * 支持超时机制和默认回复。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultHumanNode implements HumanNode {

    private final String nodeId;
    private final String prompt;
    private final long timeoutSeconds;
    private final String defaultResponse;

    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();

    /**
     * 创建人工节点
     *
     * @param nodeId          节点 ID
     * @param prompt          提示信息
     * @param timeoutSeconds  超时时间（秒），0 表示不超时
     * @param defaultResponse 超时时的默认回复
     */
    public DefaultHumanNode(@NonNull String nodeId, @NonNull String prompt,
                            long timeoutSeconds, @Nullable String defaultResponse) {
        this.nodeId = nodeId;
        this.prompt = prompt;
        this.timeoutSeconds = timeoutSeconds;
        this.defaultResponse = defaultResponse;
    }

    /**
     * 创建人工节点（不超时）
     *
     * @param nodeId 节点 ID
     * @param prompt 提示信息
     */
    public DefaultHumanNode(@NonNull String nodeId, @NonNull String prompt) {
        this(nodeId, prompt, 0, null);
    }

    @Override
    @NonNull
    public String getNodeId() {
        return nodeId;
    }

    @Override
    @NonNull
    public String getType() {
        return "human";
    }

    @Override
    @NonNull
    public NodeOutput execute(@NonNull ExecutionContext context, @NonNull Map<String, Object> params) {
        try {
            String userInput;

            if (timeoutSeconds > 0) {
                userInput = inputQueue.poll(timeoutSeconds, TimeUnit.SECONDS);
                if (userInput == null) {
                    log.warn("Human node timed out: nodeId={}, using default response", nodeId);
                    userInput = defaultResponse != null ? defaultResponse : "";
                }
            } else {
                userInput = inputQueue.take();
            }

            Map<String, Object> data = Map.of(
                    "input", userInput,
                    "prompt", prompt
            );

            return NodeOutput.of(data, nodeId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NodeOutput.of(Map.of("error", "Interrupted", "prompt", prompt), nodeId);
        }
    }

    @Override
    @NonNull
    public Flux<WorkflowNode.NodeEvent> executeStream(@NonNull ExecutionContext context, @NonNull Map<String, Object> params) {
        return Flux.create(sink -> {
            try {
                NodeOutput output = execute(context, params);
                sink.next(WorkflowNode.NodeEvent.complete(output));
                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    @Override
    @NonNull
    public String getPrompt() {
        return prompt;
    }

    @Override
    @NonNull
    public HumanInteraction getHumanInteraction() {
        return new HumanInteraction() {
            @Override
            @NonNull
            public CompletableFuture<HumanDecision> requestApproval(
                    @NonNull String interactionId, @NonNull String prompt,
                    @Nullable Object content, @NonNull Duration timeout) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        String input = inputQueue.poll(timeout.toSeconds(), TimeUnit.SECONDS);
                        if (input == null) {
                            return HumanDecision.TIMEOUT;
                        }
                        return HumanDecision.APPROVED;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return HumanDecision.CANCELLED;
                    }
                });
            }

            @Override
            @NonNull
            public CompletableFuture<Object> requestInput(
                    @NonNull String interactionId, @NonNull String prompt,
                    @Nullable Object schema, @NonNull Duration timeout) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        return inputQueue.poll(timeout.toSeconds(), TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                });
            }

            @Override
            public void submitDecision(@NonNull String interactionId, @NonNull HumanDecision decision) {
                inputQueue.offer(decision.toString());
            }

            @Override
            @NonNull
            public List<?> getPendingInteractions() {
                return List.of();
            }
        };
    }

    /**
     * 提供人工输入
     *
     * @param input 人工输入内容
     */
    public void provideInput(@NonNull String input) {
        inputQueue.offer(input);
    }
}