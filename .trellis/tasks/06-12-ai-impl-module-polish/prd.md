# ai-impl 模块精细化打磨 — ai-spring-ai + ai-langchain4j

## Goal

对 ai-impl 的两个子模块进行精细化打磨，重点：AutoConfiguration 依赖排序 + IllegalArgumentException→BusinessException。

## What I already know

- ai-langchain4j: 7 个 AutoConfiguration，2 个已有 after，5 个缺失
- ai-spring-ai: 6 个 AutoConfiguration，3 个已有 after，3 个缺失。**源码在 git 中被删除，仅存于 jar** — 需要先恢复
- ai-langchain4j 1 处 IAE→BE
- ai-spring-ai 2 处 IAE→BE
- 两个模块均无测试

## Requirements

### T0: 恢复 ai-spring-ai 源码

ai-spring-ai 的 src/main/java 下源文件被删除（仅空目录）。需要从 git 历史恢复所有 Java 源文件。

### T1: ai-langchain4j @AutoConfigureAfter

| AutoConfiguration | 添加 after |
|---|---|
| Lc4jChatAutoConfiguration | afterName = AfgAutoConfiguration + AiCoreAutoConfiguration |
| Lc4jModelAutoConfiguration | 已有 after=Lc4jChatAutoConfiguration，补充 afterName |
| Lc4jAdvisorAutoConfiguration | after = Lc4jChatAutoConfiguration.class, afterName |
| Lc4jMemoryAutoConfiguration | 已有 after=Lc4jChatAutoConfiguration，补充 afterName |
| Lc4jEmbeddingAutoConfiguration | after = Lc4jChatAutoConfiguration.class, afterName |
| Lc4jObservationAutoConfiguration | after = Lc4jChatAutoConfiguration.class, afterName |
| Lc4jToolAutoConfiguration | after = Lc4jChatAutoConfiguration.class, afterName |

afterName = {"io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration", "io.github.afgprojects.framework.ai.core.autoconfigure.AiCoreAutoConfiguration"}

### T2: ai-spring-ai @AutoConfigureAfter

| AutoConfiguration | 添加 after |
|---|---|
| SpringAiChatAutoConfiguration | afterName = AfgAutoConfiguration + AiCoreAutoConfiguration |
| SpringAiModelAutoConfiguration | 已有 after=SpringAiChatAutoConfiguration，补充 afterName |
| SpringAiAdvisorAutoConfiguration | 已有 after=SpringAiChatAutoConfiguration，补充 afterName |
| SpringAiMemoryAutoConfiguration | 已有 after=SpringAiChatAutoConfiguration，补充 afterName |
| SpringAiRagAutoConfiguration | after = SpringAiChatAutoConfiguration.class, afterName |
| SpringAiObservationAutoConfiguration | after = SpringAiChatAutoConfiguration.class, afterName |

### T3: IllegalArgumentException→BusinessException

- Lc4jModelRegistry: "Model not registered" → ENTITY_NOT_FOUND
- SpringAiModelRegistry: "Model not registered" → ENTITY_NOT_FOUND
- AiMessageConverter: "AiMedia must have either url or data" → PARAM_ERROR

## Acceptance Criteria

- [ ] ai-spring-ai 源码恢复
- [ ] 所有 AutoConfiguration 添加 @AutoConfigureAfter
- [ ] 3 处 IAE→BE 替换
- [ ] 全量构建通过
