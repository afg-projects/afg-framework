package io.github.afgprojects.framework.ai.pipeline;

import io.github.afgprojects.framework.ai.core.pipeline.ApplicationConfig;
import io.github.afgprojects.framework.ai.core.pipeline.KnowledgeSearchClient;
import io.github.afgprojects.framework.ai.core.pipeline.PipelineContext;
import io.github.afgprojects.framework.ai.core.pipeline.PipelineStep;
import io.github.afgprojects.framework.ai.core.pipeline.SearchResult;
import io.github.afgprojects.framework.ai.core.pipeline.StepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class KnowledgeSearchStep implements PipelineStep {
    private final KnowledgeSearchClient searchClient;

    @Override
    public String getName() { return "knowledgeSearch"; }

    @Override
    public int getOrder() { return 20; }

    @Override
    public StepResult execute(PipelineContext context) {
        ApplicationConfig config = context.getConfig();
        if (config.getKnowledgeIds() == null || config.getKnowledgeIds().isEmpty()) {
            return StepResult.skip("未关联知识库");
        }
        try {
            String query = context.getOptimizedQuestion();
            List<SearchResult> results = searchClient.search(
                query, config.getKnowledgeIds(), config.getSearchMode(),
                config.getSimilarityThreshold(), config.getTopN()
            );
            if (results.isEmpty()) {
                return StepResult.ok(Map.of("searchResults", List.of(), "referenceContent", "", "hasReference", false));
            }
            StringBuilder sb = new StringBuilder();
            for (SearchResult r : results) {
                if (sb.length() + r.content().length() > config.getMaxContentChars()) {
                    int remaining = config.getMaxContentChars() - sb.length();
                    if (remaining > 0) sb.append(r.content(), 0, remaining);
                    break;
                }
                sb.append(r.content()).append("\n");
            }
            return StepResult.ok(Map.of("searchResults", results, "referenceContent", sb.toString().trim(), "hasReference", true));
        } catch (Exception e) {
            log.warn("知识库检索失败，跳过: {}", e.getMessage());
            return StepResult.ok(Map.of("searchResults", List.of(), "referenceContent", "", "hasReference", false));
        }
    }
}
