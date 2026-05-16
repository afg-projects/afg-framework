package io.github.afgprojects.framework.apt.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * 通用字段配置文件加载器
 * <p>
 * 从 META-INF/afg-common-fields.json 加载项目级通用字段配置。
 */
class CommonFieldConfigLoader {

    private static final String CONFIG_LOCATION = "META-INF/afg-common-fields.json";

    private final ProcessingEnvironment processingEnv;
    private final ObjectMapper objectMapper;

    CommonFieldConfigLoader(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 加载所有配置文件中的通用字段
     * <p>
     * 扫描 classpath 中所有 META-INF/afg-common-fields.json 文件并合并。
     *
     * @return 通用字段信息列表
     */
    List<CommonFieldInfo> load() {
        List<CommonFieldInfo> fields = new ArrayList<>();

        try {
            Enumeration<URL> resources = processingEnv.getFiler()
                .getClass()
                .getClassLoader()
                .getResources(CONFIG_LOCATION);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                List<CommonFieldInfo> loaded = loadFromUrl(url);
                fields.addAll(loaded);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "Failed to scan common field configs: " + e.getMessage());
        }

        return fields;
    }

    /**
     * 从 URL 加载配置
     */
    private List<CommonFieldInfo> loadFromUrl(URL url) {
        List<CommonFieldInfo> fields = new ArrayList<>();

        try (InputStream is = url.openStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode fieldsNode = root.get("fields");

            if (fieldsNode != null && fieldsNode.isArray()) {
                for (JsonNode fieldNode : fieldsNode) {
                    CommonFieldInfo info = parseField(fieldNode, url.toString());
                    if (info != null) {
                        fields.add(info);
                    }
                }
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Failed to load common field config from " + url + ": " + e.getMessage());
        }

        return fields;
    }

    /**
     * 解析单个字段配置
     */
    private CommonFieldInfo parseField(JsonNode node, String source) {
        try {
            String name = node.get("name").asText();
            String propertyName = node.get("propertyName").asText();
            String columnName = node.has("columnName") ? node.get("columnName").asText() : "";
            String fieldType = node.get("fieldType").asText();
            boolean isId = node.has("isId") && node.get("isId").asBoolean();
            boolean isGenerated = node.has("isGenerated") && node.get("isGenerated").asBoolean();

            // 校验必填字段
            if (name == null || name.isEmpty()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Missing 'name' in common field config: " + source);
                return null;
            }
            if (propertyName == null || propertyName.isEmpty()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Missing 'propertyName' in common field config: " + source);
                return null;
            }
            if (fieldType == null || fieldType.isEmpty()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Missing 'fieldType' in common field config: " + source);
                return null;
            }

            return CommonFieldInfo.fromConfig(name, propertyName, columnName, fieldType, isId, isGenerated);
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "Failed to parse field config in " + source + ": " + e.getMessage());
            return null;
        }
    }
}
