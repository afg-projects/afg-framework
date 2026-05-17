package io.github.afgprojects.framework.ai.agent.decomposition;

import io.github.afgprojects.framework.ai.core.multiagent.decomposition.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateTaskDecomposerTest {

    private TemplateTaskDecomposer decomposer;

    @BeforeEach
    void setUp() {
        decomposer = new TemplateTaskDecomposer();

        // 注册测试模板
        decomposer.registerTemplate("code-review", "代码审查",
                List.of(
                        SubTask.of("syntax", "语法检查", "检查代码语法", SubTask.TaskType.ANALYSIS),
                        SubTask.of("logic", "逻辑检查", "检查代码逻辑", SubTask.TaskType.ANALYSIS),
                        SubTask.of("security", "安全检查", "检查安全问题", SubTask.TaskType.ANALYSIS)
                ),
                Map.of("syntax", List.of("logic", "security"))
        );
    }

    @Test
    @DisplayName("获取分解器名称")
    void getName_returnsName() {
        assertThat(decomposer.getName()).isEqualTo("template");
    }

    @Test
    @DisplayName("支持匹配的任务")
    void supports_matchingTask_returnsTrue() {
        TaskDescription task = TaskDescription.of("代码审查", "审查用户提交的代码");

        assertThat(decomposer.supports(task)).isTrue();
    }

    @Test
    @DisplayName("不支持不匹配的任务")
    void supports_nonMatchingTask_returnsFalse() {
        TaskDescription task = TaskDescription.of("数据分析", "分析销售数据");

        assertThat(decomposer.supports(task)).isFalse();
    }

    @Test
    @DisplayName("分解匹配的任务")
    void decompose_matchingTask_returnsSubTasks() {
        TaskDescription task = TaskDescription.of("代码审查", "审查代码");
        DecompositionContext context = DecompositionContext.of("wf-1");

        DecompositionResult result = decomposer.decompose(task, context);

        assertThat(result.subTasks()).hasSize(3);
        assertThat(result.subTasks().get(0).name()).isEqualTo("语法检查");
    }

    @Test
    @DisplayName("分解不匹配的任务返回单任务")
    void decompose_nonMatchingTask_returnsSingleTask() {
        TaskDescription task = TaskDescription.of("数据分析", "分析数据");
        DecompositionContext context = DecompositionContext.of("wf-1");

        DecompositionResult result = decomposer.decompose(task, context);

        assertThat(result.subTasks()).hasSize(1);
        assertThat(result.strategy()).isEqualTo("single");
    }

    @Test
    @DisplayName("返回依赖关系")
    void decompose_returnsDependencies() {
        TaskDescription task = TaskDescription.of("代码审查", "审查代码");
        DecompositionContext context = DecompositionContext.of("wf-1");

        DecompositionResult result = decomposer.decompose(task, context);

        assertThat(result.dependencies()).containsKey("syntax");
        assertThat(result.dependencies().get("syntax")).containsExactly("logic", "security");
    }
}
