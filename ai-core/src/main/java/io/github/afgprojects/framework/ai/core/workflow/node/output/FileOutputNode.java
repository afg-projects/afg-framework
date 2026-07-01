package io.github.afgprojects.framework.ai.core.workflow.node.output;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * File output node - writes data to a file.
 *
 * <p>Writes the provided content to a file on the filesystem.
 * Supports text content with configurable encoding and append mode.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.</p>
 */
@Slf4j
public class FileOutputNode extends AbstractWorkflowNode<FileOutputNode.Params> {

    public static final String TYPE = "file-output";

    /** Strongly-typed parameters for {@link FileOutputNode}. */
    public record Params(
            @Param(displayName = "File path", description = "Path to the output file", required = true)
            String filePath,
            @Param(displayName = "Content", description = "Content to write", required = true)
            String content,
            @Param(displayName = "Append", description = "Whether to append to existing file", defaultValue = "false")
            Boolean append,
            @Param(displayName = "Create parent dirs", description = "Create parent directories if needed", defaultValue = "true")
            Boolean createParentDirs
    ) {
        /** Whether to append to existing file. */
        public boolean isAppend() {
            return Boolean.TRUE.equals(append);
        }

        /** Whether to create parent directories. */
        public boolean isCreateParentDirs() {
            return createParentDirs == null || createParentDirs;
        }
    }

    /** Output descriptor for {@link FileOutputNode}. */
    public record Output(
            @Out(description = "File path") String filePath,
            @Out(description = "Bytes written") int bytesWritten,
            @Out(description = "Whether appended") boolean appended
    ) {}

    public FileOutputNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String filePath = params.filePath();
        String content = params.content();
        boolean append = params.isAppend();
        boolean createParentDirs = params.isCreateParentDirs();

        log.debug("FileOutputNode [{}] writing to: {}", getNodeId(), filePath);

        Path path = Path.of(filePath);

        try {
            if (createParentDirs) {
                Files.createDirectories(path.getParent());
            }

            if (append) {
                Files.writeString(path, content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(path, content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("filePath", filePath);
            result.put("bytesWritten", content.getBytes(StandardCharsets.UTF_8).length);
            result.put("appended", append);
            return result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }
}
