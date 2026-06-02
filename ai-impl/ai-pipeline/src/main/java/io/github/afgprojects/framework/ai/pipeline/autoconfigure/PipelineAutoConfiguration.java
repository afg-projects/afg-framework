package io.github.afgprojects.framework.ai.pipeline.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.chat.ChatClientRegistry;
import io.github.afgprojects.framework.ai.core.api.pipeline.ChatPipeline;
import io.github.afgprojects.framework.ai.core.api.pipeline.KnowledgeSearchClient;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineStep;
import io.github.afgprojects.framework.ai.pipeline.AiChatStep;
import io.github.afgprojects.framework.ai.pipeline.DefaultChatPipeline;
import io.github.afgprojects.framework.ai.pipeline.KnowledgeSearchStep;
import io.github.afgprojects.framework.ai.pipeline.MessageBuildStep;
import io.github.afgprojects.framework.ai.pipeline.QuestionOptimizeStep;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@AutoConfiguration
@ConditionalOnClass(ChatPipeline.class)
public class PipelineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatPipeline.class)
    public ChatPipeline chatPipeline(KnowledgeSearchClient searchClient,
                                      ChatClientRegistry chatClientRegistry) {
        List<PipelineStep> allSteps = new ArrayList<>();
        allSteps.add(new QuestionOptimizeStep(chatClientRegistry.getDefault()));
        allSteps.add(new KnowledgeSearchStep(searchClient));
        allSteps.add(new MessageBuildStep());
        allSteps.add(new AiChatStep(chatClientRegistry));
        allSteps.sort(Comparator.comparingInt(PipelineStep::getOrder));
        return new DefaultChatPipeline(allSteps);
    }
}
