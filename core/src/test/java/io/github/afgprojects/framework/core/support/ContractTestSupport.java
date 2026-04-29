package io.github.afgprojects.framework.core.support;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.NonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 契约测试支持类
 * 提供 API 契约验证能力
 */
public abstract class ContractTestSupport {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 加载契约文件
     *
     * @param contractFile 契约文件路径（相对于 contracts 目录）
     * @return JsonNode 表示的契约
     */
    protected JsonNode loadContract(@NonNull String contractFile) {
        String path = "contracts/" + contractFile;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Contract file not found: " + path);
            }
            return objectMapper.readTree(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load contract file: " + path, e);
        }
    }

    /**
     * 验证请求是否符合契约
     *
     * @param request      请求对象
     * @param contractFile 契约文件路径
     */
    protected void verifyRequestContract(@NonNull Object request, @NonNull String contractFile) {
        JsonNode contract = loadContract(contractFile);
        JsonNode requestNode = objectMapper.valueToTree(request);
        verifyContract(requestNode, contract, "request");
    }

    /**
     * 验证响应是否符合契约
     *
     * @param response     响应对象
     * @param contractFile 契约文件路径
     */
    protected void verifyResponseContract(@NonNull Object response, @NonNull String contractFile) {
        JsonNode contract = loadContract(contractFile);
        JsonNode responseNode = objectMapper.valueToTree(response);
        verifyContract(responseNode, contract, "response");
    }

    /**
     * 验证 JSON 节点是否符合契约
     *
     * @param actual   实际值
     * @param contract 契约定义
     * @param path     当前路径（用于错误信息）
     */
    protected void verifyContract(@NonNull JsonNode actual, @NonNull JsonNode contract, @NonNull String path) {
        if (contract.isObject()) {
            verifyObjectContract(actual, contract, path);
        } else if (contract.isArray()) {
            verifyArrayContract(actual, contract, path);
        }
    }

    private void verifyObjectContract(@NonNull JsonNode actual, @NonNull JsonNode contract, @NonNull String path) {
        contract.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldContract = entry.getValue();
            String fieldPath = path + "." + fieldName;

            JsonNode actualField = actual.get(fieldName);

            // 检查必填字段
            boolean required = fieldContract.has("required")
                    && fieldContract.get("required").asBoolean();
            if (required && actualField == null) {
                throw new ContractViolationException("Missing required field: " + fieldPath);
            }

            if (actualField != null) {
                // 检查类型
                if (fieldContract.has("type")) {
                    String expectedType = fieldContract.get("type").asText();
                    verifyType(actualField, expectedType, fieldPath);
                }

                // 检查嵌套结构
                if (fieldContract.has("properties")) {
                    verifyContract(actualField, fieldContract.get("properties"), fieldPath);
                }
            }
        });
    }

    private void verifyArrayContract(@NonNull JsonNode actual, @NonNull JsonNode contract, @NonNull String path) {
        if (!actual.isArray()) {
            throw new ContractViolationException("Expected array at " + path + ", but got " + actual.getNodeType());
        }

        // 如果契约定义了数组项的结构，验证每个元素
        if (contract.size() > 0) {
            JsonNode itemContract = contract.get(0);
            for (int i = 0; i < actual.size(); i++) {
                verifyContract(actual.get(i), itemContract, path + "[" + i + "]");
            }
        }
    }

    private void verifyType(@NonNull JsonNode value, @NonNull String expectedType, @NonNull String path) {
        boolean matches =
                switch (expectedType) {
                    case "string" -> value.isTextual();
                    case "number" -> value.isNumber();
                    case "integer" -> value.isInt();
                    case "boolean" -> value.isBoolean();
                    case "array" -> value.isArray();
                    case "object" -> value.isObject();
                    default -> true;
                };

        if (!matches) {
            throw new ContractViolationException(
                    "Type mismatch at " + path + ": expected " + expectedType + ", got " + value.getNodeType());
        }
    }

    /**
     * 契约违反异常
     */
    public static class ContractViolationException extends RuntimeException {
        public ContractViolationException(@NonNull String message) {
            super(message);
        }
    }
}
