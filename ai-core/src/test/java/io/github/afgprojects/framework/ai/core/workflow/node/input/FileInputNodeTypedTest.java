package io.github.afgprojects.framework.ai.core.workflow.node.input;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamSchema;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;
import io.github.afgprojects.framework.ai.core.workflow.engine.DefaultExecutionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 FileInputNode 迁移到强类型 record 后的行为：
 * schema 自描述（反射 @Param）、强类型绑定、默认值填充、必填校验。
 *
 * <p>批次 2 review gate：确认 INPUT 节点的 R1（schema 与执行打通）/ R2（自描述）落地。</p>
 */
@DisplayName("FileInputNode (typed)")
class FileInputNodeTypedTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("getParamSchema 应从 @Param 反射出 filePath 与 encoding")
    void shouldReflectParamSchema() {
        FileInputNode node = new FileInputNode("file-1");

        Map<String, ParamSchema> schema = node.getParamSchema();

        assertThat(schema).containsKeys("filePath", "encoding");
        ParamSchema filePath = schema.get("filePath");
        assertThat(filePath.required()).isTrue();
        assertThat(filePath.type()).isEqualTo(ParamType.STRING);
        assertThat(schema.get("encoding").defaultValue()).isEqualTo("UTF-8");
    }

    @Test
    @DisplayName("应读取文件内容并返回 content/fileName/fileSize")
    void shouldReadFileContent() throws Exception {
        Path file = tempDir.resolve("hello.txt");
        Files.writeString(file, "hello world");

        FileInputNode node = new FileInputNode("file-1");
        Map<String, Object> params = Map.of("filePath", file.toString());

        NodeOutput output = node.execute(new DefaultExecutionContext("wf", "c", "u"), params);

        assertThat(output.data()).containsEntry("content", "hello world");
        assertThat(output.data()).containsEntry("fileName", "hello.txt");
        assertThat(output.data()).containsEntry("isBinary", false);
        assertThat(output.data()).containsKey("fileSize");
    }

    @Test
    @DisplayName("缺失必填 filePath 时应返回可定位的错误输出（非抛异常到引擎）")
    void shouldReturnErrorWhenRequiredMissing() {
        FileInputNode node = new FileInputNode("file-1");

        // 空 params：binder 应因 filePath 缺失而失败，基类捕获为错误输出
        NodeOutput output = node.execute(new DefaultExecutionContext("wf", "c", "u"), Map.of());

        assertThat(output.data()).containsKey("error");
        assertThat((String) output.data().get("error")).contains("filePath");
    }
}
