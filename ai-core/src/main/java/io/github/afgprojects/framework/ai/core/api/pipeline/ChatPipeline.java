package io.github.afgprojects.framework.ai.core.api.pipeline;

import org.jspecify.annotations.NonNull;
import reactor.core.publisher.Flux;

public interface ChatPipeline {
    @NonNull PipelineResult execute(@NonNull PipelineContext context);
    @NonNull Flux<String> executeStream(@NonNull PipelineContext context);
}
