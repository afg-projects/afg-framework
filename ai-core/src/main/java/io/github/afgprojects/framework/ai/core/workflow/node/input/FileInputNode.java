package io.github.afgprojects.framework.ai.core.workflow.node.input;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * File input node - reads file content and provides it as workflow data.
 *
 * <p>Reads a file from the configured path and outputs its content.
 * Supports text files with configurable encoding. Binary files are
 * represented as a Base64-encoded string in the output.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing:
 * the parameter schema, validation, and execution all read from the same record.</p>
 */
@Slf4j
public class FileInputNode extends AbstractWorkflowNode<FileInputNode.Params> {

    public static final String TYPE = "file-input";

    /** Strongly-typed parameters for {@link FileInputNode}. */
    public record Params(
            @Param(displayName = "File path", description = "Path to the file to read", required = true)
            String filePath,
            @Param(displayName = "File encoding", description = "File encoding", defaultValue = "UTF-8")
            String encoding
    ) {
        /** Normalized encoding, defaulting to UTF-8 when absent. */
        public String effectiveEncoding() {
            return encoding == null || encoding.isBlank() ? "UTF-8" : encoding;
        }
    }

    /** Output descriptor for {@link FileInputNode}. */
    public record Output(
            @Out(description = "File path") String filePath,
            @Out(description = "File name") String fileName,
            @Out(description = "File content") String content,
            @Out(description = "File size") long fileSize,
            @Out(description = "Whether binary") boolean isBinary
    ) {}

    public FileInputNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String filePath = params.filePath();
        String encoding = params.effectiveEncoding();

        log.debug("FileInputNode [{}] reading file: {}", getNodeId(), filePath);

        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filePath", filePath);
        result.put("fileName", path.getFileName().toString());

        try {
            result.put("fileSize", Files.size(path));
            String content = Files.readString(path, Charset.forName(encoding));
            result.put("content", content);
            result.put("isBinary", false);
        } catch (IOException e) {
            // If text reading fails, read as binary and Base64-encode
            try {
                byte[] bytes = Files.readAllBytes(path);
                result.put("content", java.util.Base64.getEncoder().encodeToString(bytes));
                result.put("isBinary", true);
                result.put("fileSize", bytes.length);
            } catch (IOException ex) {
                RuntimeException runtimeEx = new RuntimeException("Failed to read file: " + filePath, ex);
                runtimeEx.addSuppressed(e);
                throw runtimeEx;
            }
        }

        return result;
    }
}
