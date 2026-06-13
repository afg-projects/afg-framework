package io.github.afgprojects.framework.ai.core.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolExecutionException;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.tool.annotation.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Tool 注解扫描注册器。
 * <p>
 * 在 Spring 上下文刷新完成后，扫描所有标注了 {@link io.github.afgprojects.framework.ai.core.tool.annotation.Tool}
 * 注解的方法，将其适配为 {@link Tool} 接口实现并注册到 {@link ToolRegistry}。
 *
 * @author afg-projects
 * @since 1.0.0
 * @see io.github.afgprojects.framework.ai.core.tool.annotation.Tool
 * @see ToolRegistry
 */
@Slf4j
@RequiredArgsConstructor
public class ToolRegistrar {

    private final ToolRegistry toolRegistry;

    /**
     * 监听上下文刷新事件，扫描并注册所有 @Tool 方法。
     *
     * @param event 上下文刷新事件
     */
    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        Map<String, Object> beans = new HashMap<>();
        beans.putAll(context.getBeansWithAnnotation(org.springframework.stereotype.Component.class));
        beans.putAll(context.getBeansWithAnnotation(org.springframework.stereotype.Service.class));

        int registered = 0;
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            registered += scanAndRegister(entry.getValue());
        }

        log.info("ToolRegistrar: scanned and registered {} tool(s) from @Tool annotations", registered);
    }

    /**
     * 扫描单个 Bean 中的 @Tool 注解方法并注册。
     *
     * @param bean Spring Bean 实例
     * @return 注册的 Tool 数量
     */
    int scanAndRegister(Object bean) {
        int count = 0;
        Method[] methods = bean.getClass().getDeclaredMethods();

        for (Method method : methods) {
            io.github.afgprojects.framework.ai.core.tool.annotation.Tool toolAnnotation =
                    AnnotationUtils.findAnnotation(method, io.github.afgprojects.framework.ai.core.tool.annotation.Tool.class);
            if (toolAnnotation == null) {
                continue;
            }

            AnnotatedMethodTool tool = new AnnotatedMethodTool(bean, method, toolAnnotation);
            toolRegistry.registerOrReplace(tool);
            count++;

            log.debug("Registered @Tool method: {} -> {}", method.getName(), tool.name());
        }

        return count;
    }

    /**
     * 基于 @Tool 注解方法的 Tool 适配器。
     * <p>
     * 将标注了 {@link io.github.afgprojects.framework.ai.core.tool.annotation.Tool} 的方法
     * 适配为 {@link Tool} 接口，支持 AI 模型的 function calling。
     */
    static class AnnotatedMethodTool implements Tool<Map<String, Object>, Object> {

        private final Object bean;
        private final Method method;
        private final io.github.afgprojects.framework.ai.core.tool.annotation.Tool annotation;
        private final String toolName;
        private final String toolDescription;
        private final String inputSchemaJson;

        AnnotatedMethodTool(Object bean, Method method,
                            io.github.afgprojects.framework.ai.core.tool.annotation.Tool annotation) {
            this.bean = bean;
            this.method = method;
            this.annotation = annotation;
            this.toolName = annotation.name();
            this.toolDescription = annotation.description().isEmpty()
                    ? method.getName() : annotation.description();
            this.inputSchemaJson = buildInputSchema(method);
        }

        @Override
        public String name() {
            return toolName;
        }

        @Override
        public String description() {
            return toolDescription;
        }

        @Override
        public String inputSchema() {
            return inputSchemaJson;
        }

        @Override
        public @Nullable TypeReference<Map<String, Object>> inputType() {
            return new TypeReference<>() {};
        }

        @Override
        public Object execute(Map<String, Object> input) {
            try {
                Object[] args = resolveArguments(input);
                method.setAccessible(true);
                return method.invoke(bean, args);
            } catch (ToolExecutionException e) {
                throw e;
            } catch (Exception e) {
                Throwable cause = e;
                if (e.getCause() != null) {
                    cause = e.getCause();
                }
                throw new ToolExecutionException(
                        "Tool '" + toolName + "' execution failed: " + cause.getMessage(), cause);
            }
        }

        /**
         * 将 Map 输入解析为方法参数数组。
         */
        private Object[] resolveArguments(Map<String, Object> input) {
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                ToolParam toolParam = param.getAnnotation(ToolParam.class);
                String paramName = toolParam != null ? toolParam.name() : param.getName();

                Object value = input.get(paramName);

                // 如果未从 input 中获取到值，尝试使用默认值
                if (value == null && toolParam != null && !toolParam.defaultValue().isEmpty()) {
                    value = convertValue(toolParam.defaultValue(), param.getType());
                }

                // 类型转换
                if (value != null && !param.getType().isInstance(value)) {
                    value = convertValue(value, param.getType());
                }

                args[i] = value;
            }

            return args;
        }

        /**
         * 构建 JSON Schema 描述方法参数。
         */
        private String buildInputSchema(Method m) {
            Parameter[] parameters = m.getParameters();
            if (parameters.length == 0) {
                return "{}";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"object\",\"properties\":{");

            List<String> required = new ArrayList<>();
            boolean first = true;

            for (Parameter param : parameters) {
                ToolParam toolParam = param.getAnnotation(ToolParam.class);
                String paramName = toolParam != null ? toolParam.name() : param.getName();
                String paramDesc = toolParam != null ? toolParam.description() : "";

                if (!first) {
                    sb.append(",");
                }
                first = false;

                sb.append("\"").append(paramName).append("\":{");
                sb.append("\"type\":\"").append(mapToJsonType(param.getType())).append("\"");
                if (!paramDesc.isEmpty()) {
                    sb.append(",\"description\":\"").append(escapeJson(paramDesc)).append("\"");
                }
                sb.append("}");

                if (toolParam == null || toolParam.required()) {
                    required.add(paramName);
                }
            }

            sb.append("}");

            if (!required.isEmpty()) {
                sb.append(",\"required\":[");
                sb.append(required.stream().map(r -> "\"" + r + "\"").collect(Collectors.joining(",")));
                sb.append("]");
            }

            sb.append("}");
            return sb.toString();
        }

        /**
         * 将 Java 类型映射到 JSON Schema 类型。
         */
        private String mapToJsonType(Class<?> type) {
            if (type == String.class) return "string";
            if (type == Integer.class || type == int.class
                    || type == Long.class || type == long.class) return "integer";
            if (type == Double.class || type == double.class
                    || type == Float.class || type == float.class
                    || type == Number.class) return "number";
            if (type == Boolean.class || type == boolean.class) return "boolean";
            if (type.isArray() || Collection.class.isAssignableFrom(type)) return "array";
            if (Map.class.isAssignableFrom(type)) return "object";
            return "string";
        }

        /**
         * 转换值到目标类型。
         */
        private Object convertValue(Object value, Class<?> targetType) {
            if (value == null) return null;
            if (targetType.isInstance(value)) return value;

            String strValue = String.valueOf(value);

            if (targetType == String.class) return strValue;
            if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(strValue);
            if (targetType == Long.class || targetType == long.class) return Long.parseLong(strValue);
            if (targetType == Double.class || targetType == double.class) return Double.parseDouble(strValue);
            if (targetType == Float.class || targetType == float.class) return Float.parseFloat(strValue);
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(strValue);

            return value;
        }

        /**
         * 转义 JSON 字符串中的特殊字符。
         */
        private String escapeJson(String value) {
            return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }
}
