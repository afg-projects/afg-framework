package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import io.github.afgprojects.framework.ai.core.workflow.node.input.InputNode;
import io.github.afgprojects.framework.ai.core.workflow.node.output.LogOutputNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 DefaultNodeFactory 的按 type 实例化链路：注册、解析、未知类型返回 null、
 * 以及作为 Function&lt;String,WorkflowNode&gt; 的 resolver 视图。
 *
 * <p>批次 1 的 review gate：确认装配链路打通（无依赖节点可被引擎按 type 创建）。</p>
 */
@DisplayName("DefaultNodeFactory")
class DefaultNodeFactoryTest {

    @Test
    @DisplayName("已注册的无依赖节点类型应能按 type 创建实例")
    void shouldCreateNodeByType() {
        DefaultNodeFactory factory = new DefaultNodeFactory();
        BuiltinNodeFactoryRegistrar.registerAll(factory);

        WorkflowNode input = factory.create(InputNode.TYPE, "input-1");
        WorkflowNode log = factory.create(LogOutputNode.TYPE, "log-1");

        assertThat(input).isInstanceOf(InputNode.class);
        assertThat(input.getNodeId()).isEqualTo("input-1");
        assertThat(input.getType()).isEqualTo(InputNode.TYPE);
        assertThat(log).isInstanceOf(LogOutputNode.class);
        assertThat(log.getNodeId()).isEqualTo("log-1");
    }

    @Test
    @DisplayName("apply(type) resolver 视图应返回带派生 nodeId 的实例")
    void shouldBeUsableAsResolver() {
        DefaultNodeFactory factory = new DefaultNodeFactory();
        BuiltinNodeFactoryRegistrar.registerAll(factory);

        WorkflowNode node = factory.apply(InputNode.TYPE);

        assertThat(node).isNotNull();
        assertThat(node.getType()).isEqualTo(InputNode.TYPE);
    }

    @Test
    @DisplayName("未知 type 应返回 null（引擎会跳过，与重构前行为一致）")
    void shouldReturnNullForUnknownType() {
        DefaultNodeFactory factory = new DefaultNodeFactory();

        assertThat(factory.create("does-not-exist", "x")).isNull();
        assertThat(factory.apply("does-not-exist")).isNull();
    }

    @Test
    @DisplayName("registerAll 应注册全部无依赖内置节点类型")
    void shouldRegisterAllDependencyFreeFactories() {
        DefaultNodeFactory factory = new DefaultNodeFactory();
        BuiltinNodeFactoryRegistrar.registerAll(factory);

        // 抽样校验几个不同类别的无依赖节点都已注册
        assertThat(factory.get("input")).isPresent();
        assertThat(factory.get("log-output")).isPresent();
        assertThat(factory.get("mapping")).isPresent();
        assertThat(factory.get("delay")).isPresent();
        assertThat(factory.get("database-write")).isPresent();
    }
}
