package io.github.afgprojects.framework.ai.core.workflow.node.output;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
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
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code filePath} (required) - path to the output file</li>
 *   <li>{@code content} (required) - content to write</li>
 *   <li>{@code encoding} (optional) - file encoding, defaults to UTF-8</li>
 *   <li>{@code append} (optional) - whether to append to existing file, defaults to false</li>
 *   <li>{@code createParentDirs} (optional) - create parent directories if needed, defaults to true</li>
 * </ul>
 */
@Slf4j
public class FileOutputNode extends AbstractWorkflowNode {

    public static final String TYPE = "file-output";

    public FileOutputNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String filePath = getRequiredParam(params, "filePath");
        String content = getRequiredParam(params, "content");
        boolean append = getBooleanParam(params, "append", false);
        boolean createParentDirs = getBooleanParam(params, "createParentDirs", true);

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

    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value.toString();
    }

    private boolean getBooleanParam(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(value.toString());
    }
}
