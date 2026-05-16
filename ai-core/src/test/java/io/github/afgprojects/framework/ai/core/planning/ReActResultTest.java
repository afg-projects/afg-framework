package io.github.afgprojects.framework.ai.core.planning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReActResult 单元测试
 */
class ReActResultTest {

    @Test
    @DisplayName("创建成功结果")
    void success() {
        List<Object> steps = List.of(Map.of("step", 1));
        ReActResult result = ReActResult.success("Answer", steps);

        assertThat(result.success()).isTrue();
        assertThat(result.answer()).isEqualTo("Answer");
        assertThat(result.steps()).isEqualTo(steps);
    }

    @Test
    @DisplayName("创建失败结果")
    void failure() {
        List<Object> steps = List.of(Map.of("step", 1));
        ReActResult result = ReActResult.failure("Error", steps);

        assertThat(result.success()).isFalse();
        assertThat(result.answer()).isEqualTo("Error");
    }

    @Test
    @DisplayName("创建空步骤的成功结果")
    void completed() {
        ReActResult result = ReActResult.completed("Answer");

        assertThat(result.success()).isTrue();
        assertThat(result.answer()).isEqualTo("Answer");
        assertThat(result.hasSteps()).isFalse();
    }

    @Test
    @DisplayName("hasSteps 返回正确结果")
    void hasSteps() {
        ReActResult withSteps = ReActResult.success("Answer", List.of("step"));
        ReActResult withoutSteps = ReActResult.completed("Answer");

        assertThat(withSteps.hasSteps()).isTrue();
        assertThat(withoutSteps.hasSteps()).isFalse();
    }

    @Test
    @DisplayName("stepCount 返回正确数量")
    void stepCount() {
        ReActResult result = ReActResult.success("Answer", List.of("a", "b", "c"));

        assertThat(result.stepCount()).isEqualTo(3);
    }
}