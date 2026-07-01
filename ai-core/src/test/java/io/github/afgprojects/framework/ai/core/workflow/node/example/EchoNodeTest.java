package io.github.afgprojects.framework.ai.core.workflow.node.example;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamSchema;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;
import io.github.afgprojects.framework.ai.core.workflow.engine.DefaultExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.DefaultNodeFactory;
import io.github.afgprojects.framework.ai.core.workflow.node.NodeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R5 扩展性验证：EchoNode 作为示例自定义节点，证明「声明 Params record + 继承
 * AbstractWorkflowNode + 注册 NodeFactory」即可获得完整的 schema 自描述、强类型
 * 绑定、默认值填充、必填校验、引擎实例化能力——无需任何手写样板。
 */
@DisplayName("EchoNode (R5 扩展性验证)")
class EchoNodeTest {

    @Test
    @DisplayName("应从 @Param 反射出 schema，message 必填、prefix 有默认值")
    void shouldReflectSelfDescribingSchema() {
        EchoNode node = new EchoNode("echo-1");

        Map<String, ParamSchema> schema = node.getParamSchema();

        assertThat(schema).containsKeys("message", "prefix");
        assertThat(schema.get("message").required()).isTrue();
        assertThat(schema.get("message").type()).isEqualTo(ParamType.STRING);
        assertThat(schema.get("prefix").required()).isFalse();
        assertThat(schema.get("prefix").defaultValue()).isEqualTo("echo: ");
    }

    @Test
    @DisplayName("应强类型绑定参数并应用 prefix 默认值")
    void shouldBindTypedParamsWithDefault() {
        EchoNode node = new EchoNode("echo-1");

        NodeOutput output = node.execute(
                new DefaultExecutionContext("wf", "c", "u"),
                Map.of("message", "hello"));

        assertThat(output.data()).containsEntry("text", "echo: hello");
        assertThat(output.data()).containsEntry("original", "hello");
    }

    @Test
    @DisplayName("自定义 prefix 应覆盖默认值")
    void shouldAcceptCustomPrefix() {
        EchoNode node = new EchoNode("echo-1");

        NodeOutput output = node.execute(
                new DefaultExecutionContext("wf", "c", "u"),
                Map.of("message", "hi", "prefix", ">> "));

        assertThat(output.data()).containsEntry("text", ">> hi");
    }

    @Test
    @DisplayName("缺失必填 message 时应返回可定位的错误输出")
    void shouldReturnLocatedErrorWhenRequiredMissing() {
        EchoNode node = new EchoNode("echo-1");

        NodeOutput output = node.execute(
                new DefaultExecutionContext("wf", "c", "u"), Map.of());

        assertThat(output.data()).containsKey("error");
        assertThat((String) output.data().get("error")).contains("message");
    }

    @Test
    @DisplayName("注册到 DefaultNodeFactory 后应能被按 type 实例化（引擎可用）")
    void shouldBeResolvableByFactoryByType() {
        DefaultNodeFactory factory = new DefaultNodeFactory();
        // 自定义节点注册：仅一条 NodeFactory 条目
        factory.register(new NodeFactory() {
            @Override
            public String type() { return EchoNode.TYPE; }
            @Override
            public WorkflowNode create(String nodeId) { return new EchoNode(nodeId); }
        });

        WorkflowNode resolved = factory.apply(EchoNode.TYPE);

        assertThat(resolved).isInstanceOf(EchoNode.class);
        assertThat(resolved.getType()).isEqualTo("echo");
        // 通过 factory 解析出的实例应能正常执行
        NodeOutput output = resolved.execute(
                new DefaultExecutionContext("wf", "c", "u"),
                Map.of("message", "via-factory"));
        assertThat(output.data()).containsEntry("text", "echo: via-factory");
    }
}
