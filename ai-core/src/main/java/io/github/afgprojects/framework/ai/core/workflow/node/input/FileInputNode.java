package io.github.afgprojects.framework.ai.core.workflow.node.input;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code filePath} (required) - path to the file to read</li>
 *   <li>{@code encoding} (optional) - file encoding, defaults to UTF-8</li>
 * </ul>
 */
@Slf4j
public class FileInputNode extends AbstractWorkflowNode {

    public static final String TYPE = "file-input";

    public FileInputNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String filePath = getRequiredParam(params, "filePath");
        String encoding = getParam(params, "encoding", "UTF-8");

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

    @SuppressWarnings("SameParameterValue")
    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value.toString();
    }
}
