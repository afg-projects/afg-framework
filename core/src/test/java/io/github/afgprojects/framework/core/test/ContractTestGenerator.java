package io.github.afgprojects.framework.core.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 契约测试生成器
 * 根据 Controller 类自动生成契约文件
 *
 * <p>使用示例:
 * <pre>{@code
 * // 生成单个 Controller 的契约
 * ContractTestGenerator.generateContract(UserController.class, "contracts/user-api");
 *
 * // 生成多个 Controller 的契约
 * ContractTestGenerator.generateContracts("contracts", UserController.class, OrderController.class);
 *
 * // 自定义配置
 * ContractTestGenerator.builder()
 *     .outputDir("contracts")
 *     .version("1.0.0")
 *     .build()
 *     .generate(UserController.class);
 * }</pre>
 */
public class ContractTestGenerator {

    private final String outputDir;
    private final String version;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    private ContractTestGenerator(Builder builder) {
        this.outputDir = builder.outputDir;
        this.version = builder.version;
        this.baseUrl = builder.baseUrl;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 生成单个 Controller 的契约文件
     *
     * @param controllerClass Controller 类
     * @param outputFile      输出文件路径
     */
    public static void generateContract(@NonNull Class<?> controllerClass, @NonNull String outputFile) {
        builder().build().generate(controllerClass, outputFile);
    }

    /**
     * 批量生成契约文件
     *
     * @param outputDir        输出目录
     * @param controllerClasses Controller 类列表
     */
    public static void generateContracts(@NonNull String outputDir, @NonNull Class<?>... controllerClasses) {
        ContractTestGenerator generator = builder().outputDir(outputDir).build();
        for (Class<?> controllerClass : controllerClasses) {
            generator.generate(controllerClass);
        }
    }

    /**
     * 生成契约文件
     *
     * @param controllerClass Controller 类
     */
    public void generate(@NonNull Class<?> controllerClass) {
        String fileName = getControllerName(controllerClass).toLowerCase().replace("controller", "") + "-api.json";
        String filePath = Paths.get(outputDir, fileName).toString();
        generate(controllerClass, filePath);
    }

    /**
     * 生成契约文件到指定路径
     *
     * @param controllerClass Controller 类
     * @param outputFile      输出文件路径
     */
    public void generate(@NonNull Class<?> controllerClass, @NonNull String outputFile) {
        try {
            // 确保输出目录存在
            Path path = Paths.get(outputFile);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            Map<String, Object> contract = buildContract(controllerClass);

            // 写入文件
            try (FileWriter writer = new FileWriter(outputFile)) {
                objectMapper.writeValue(writer, contract);
            }

            System.out.println("Contract generated: " + outputFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate contract: " + outputFile, e);
        }
    }

    /**
     * 构建契约内容
     */
    private Map<String, Object> buildContract(@NonNull Class<?> controllerClass) {
        Map<String, Object> contract = new HashMap<>();

        // 基本信息
        contract.put("openapi", "3.0.0");
        contract.put("info", buildInfo(controllerClass));
        contract.put("servers", buildServers());
        contract.put("paths", buildPaths(controllerClass));
        contract.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return contract;
    }

    /**
     * 构建 API 信息
     */
    private Map<String, Object> buildInfo(@NonNull Class<?> controllerClass) {
        Map<String, Object> info = new HashMap<>();
        info.put("title", getControllerName(controllerClass) + " API");
        info.put("version", version);
        info.put("description", "Auto-generated API contract for " + controllerClass.getSimpleName());
        return info;
    }

    /**
     * 构建服务器列表
     */
    private List<Map<String, Object>> buildServers() {
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new HashMap<>();
        server.put("url", baseUrl);
        server.put("description", "API Server");
        servers.add(server);
        return servers;
    }

    /**
     * 构建路径定义
     */
    private Map<String, Object> buildPaths(@NonNull Class<?> controllerClass) {
        Map<String, Object> paths = new HashMap<>();

        // 获取类级别的 RequestMapping
        String classPath = getClassLevelPath(controllerClass);

        // 遍历所有方法
        for (Method method : controllerClass.getDeclaredMethods()) {
            Map<String, Object> pathItem = buildPathItem(method, classPath);
            if (pathItem != null) {
                String fullPath = getMethodPath(method, classPath);
                paths.merge(fullPath, pathItem, (existing, newItem) -> {
                    if (existing instanceof Map) {
                        ((Map<String, Object>) existing).putAll((Map<String, Object>) newItem);
                    }
                    return existing;
                });
            }
        }

        return paths;
    }

    /**
     * 构建单个路径项
     */
    private Map<String, Object> buildPathItem(@NonNull Method method, @NonNull String classPath) {
        String httpMethod = getHttpMethod(method);
        if (httpMethod == null) {
            return null;
        }

        Map<String, Object> pathItem = new HashMap<>();
        Map<String, Object> operation = new HashMap<>();

        operation.put("summary", method.getName());
        operation.put("operationId", method.getName());
        operation.put("tags", List.of(getControllerName(method.getDeclaringClass())));

        // 请求参数
        List<Map<String, Object>> parameters = buildParameters(method);
        if (!parameters.isEmpty()) {
            operation.put("parameters", parameters);
        }

        // 请求体
        Map<String, Object> requestBody = buildRequestBody(method);
        if (requestBody != null) {
            operation.put("requestBody", requestBody);
        }

        // 响应
        operation.put("responses", buildResponses(method));

        pathItem.put(httpMethod.toLowerCase(), operation);
        return pathItem;
    }

    /**
     * 构建参数列表
     */
    private List<Map<String, Object>> buildParameters(@NonNull Method method) {
        List<Map<String, Object>> parameters = new ArrayList<>();

        for (Parameter param : method.getParameters()) {
            Map<String, Object> parameter = new HashMap<>();
            parameter.put("name", param.getName());
            parameter.put("in", "query"); // 默认 query
            parameter.put("required", true);
            parameter.put("schema", buildSchema(param.getType()));
            parameters.add(parameter);
        }

        return parameters;
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(@NonNull Method method) {
        // 查找第一个非基本类型参数作为请求体
        for (Parameter param : method.getParameters()) {
            if (!param.getType().isPrimitive() && !param.getType().getName().startsWith("java.lang")) {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("required", true);
                Map<String, Object> content = new HashMap<>();
                Map<String, Object> mediaType = new HashMap<>();
                mediaType.put("schema", buildSchema(param.getType()));
                content.put("application/json", mediaType);
                requestBody.put("content", content);
                return requestBody;
            }
        }
        return null;
    }

    /**
     * 构建响应定义
     */
    private Map<String, Object> buildResponses(@NonNull Method method) {
        Map<String, Object> responses = new HashMap<>();

        // 成功响应
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("description", "Success");
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> mediaType = new HashMap<>();
        mediaType.put("schema", buildSchema(method.getReturnType()));
        content.put("application/json", mediaType);
        successResponse.put("content", content);
        responses.put("200", successResponse);

        // 错误响应
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("description", "Error");
        responses.put("default", errorResponse);

        return responses;
    }

    /**
     * 构建类型 Schema
     */
    private Map<String, Object> buildSchema(@NonNull Class<?> type) {
        Map<String, Object> schema = new HashMap<>();

        if (type == String.class) {
            schema.put("type", "string");
        } else if (type == Integer.class || type == int.class) {
            schema.put("type", "integer");
        } else if (type == Long.class || type == long.class) {
            schema.put("type", "integer");
            schema.put("format", "int64");
        } else if (type == Double.class || type == double.class) {
            schema.put("type", "number");
        } else if (type == Boolean.class || type == boolean.class) {
            schema.put("type", "boolean");
        } else if (type == LocalDateTime.class) {
            schema.put("type", "string");
            schema.put("format", "date-time");
        } else if (List.class.isAssignableFrom(type)) {
            schema.put("type", "array");
            schema.put("items", Map.of("type", "object"));
        } else if (Map.class.isAssignableFrom(type)) {
            schema.put("type", "object");
        } else if (type == Void.class || type == void.class) {
            schema.put("type", "object");
        } else {
            // 复杂对象
            schema.put("type", "object");
            schema.put("$ref", "#/components/schemas/" + type.getSimpleName());
        }

        return schema;
    }

    /**
     * 获取 HTTP 方法
     */
    private String getHttpMethod(@NonNull Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            return "GET";
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            return "POST";
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            return "PUT";
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            return "PATCH";
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            return "DELETE";
        }
        return null;
    }

    /**
     * 获取方法路径
     */
    private String getMethodPath(@NonNull Method method, @NonNull String classPath) {
        String methodPath = "";

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);

        if (getMapping != null && getMapping.value().length > 0) {
            methodPath = getMapping.value()[0];
        } else if (postMapping != null && postMapping.value().length > 0) {
            methodPath = postMapping.value()[0];
        } else if (putMapping != null && putMapping.value().length > 0) {
            methodPath = putMapping.value()[0];
        } else if (patchMapping != null && patchMapping.value().length > 0) {
            methodPath = patchMapping.value()[0];
        } else if (deleteMapping != null && deleteMapping.value().length > 0) {
            methodPath = deleteMapping.value()[0];
        }

        return classPath + methodPath;
    }

    /**
     * 获取类级别路径
     */
    private String getClassLevelPath(@NonNull Class<?> controllerClass) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        if (requestMapping != null && requestMapping.value().length > 0) {
            return requestMapping.value()[0];
        }
        return "";
    }

    /**
     * 获取 Controller 名称
     */
    private String getControllerName(@NonNull Class<?> controllerClass) {
        return controllerClass.getSimpleName();
    }

    /**
     * 构建器
     */
    public static class Builder {

        private String outputDir = "contracts";
        private String version = "1.0.0";
        private String baseUrl = "http://localhost:8080";

        public Builder outputDir(@NonNull String outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        public Builder version(@NonNull String version) {
            this.version = version;
            return this;
        }

        public Builder baseUrl(@NonNull String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public ContractTestGenerator build() {
            return new ContractTestGenerator(this);
        }
    }
}
