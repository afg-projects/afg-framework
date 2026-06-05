package io.github.afgprojects.framework.governance.client.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.config.AfgConfigRegistry;
import io.github.afgprojects.framework.governance.client.config.GovernanceConfigClient;
import io.github.afgprojects.framework.governance.client.properties.config.GovernanceConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置自动上报组件
 * <p>
 * 在应用启动完成后，自动将配置上报到服务治理中心。
 *
 * @author afg-projects
 */
@Slf4j
public class ConfigAutoRegistrar implements SmartLifecycle {

    private final AfgConfigRegistry configRegistry;
    private final Environment environment;
    private final GovernanceConfigClient configClient;
    private final GovernanceConfigProperties properties;
    private final ObjectMapper objectMapper;

    private volatile boolean running = false;
    private final Map<String, ConfigMetadata> metadataCache = new HashMap<>();
    private final Map<String, String> metadataDescriptions = new HashMap<>();

    private static class ConfigMetadata {
        String name;
        String type;
        String description;
        Object defaultValue;
        boolean deprecated;

        ConfigMetadata(String name, String type, String description, Object defaultValue, boolean deprecated) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.defaultValue = defaultValue;
            this.deprecated = deprecated;
        }
    }

    public ConfigAutoRegistrar(AfgConfigRegistry configRegistry,
                               Environment environment,
                               GovernanceConfigClient configClient,
                               GovernanceConfigProperties properties,
                               ObjectMapper objectMapper) {
        this.configRegistry = configRegistry;
        this.environment = environment;
        this.configClient = configClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        loadConfigurationMetadata();
    }

    private void loadConfigurationMetadata() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:META-INF/spring-configuration-metadata.json");

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    Map<String, Object> metadata = objectMapper.readValue(is, new TypeReference<>() {});
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> props = (List<Map<String, Object>>) metadata.get("properties");
                    if (props != null) {
                        for (Map<String, Object> prop : props) {
                            String name = (String) prop.get("name");
                            if (name != null) {
                                String type = (String) prop.get("type");
                                String description = (String) prop.get("description");
                                Object defaultValue = prop.get("defaultValue");
                                Boolean deprecatedObj = (Boolean) prop.get("deprecated");
                                boolean deprecated = deprecatedObj != null && deprecatedObj;

                                ConfigMetadata configMetadata = new ConfigMetadata(
                                        name, type, description, defaultValue, deprecated);
                                metadataCache.put(name, configMetadata);

                                if (description != null) {
                                    metadataDescriptions.put(name, description);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    log.debug("Failed to load metadata from {}: {}", resource.getDescription(), e.getMessage());
                }
            }

            log.info("Loaded {} configuration metadata entries from Spring Boot metadata", metadataCache.size());
        } catch (IOException e) {
            log.debug("No Spring Boot configuration metadata found: {}", e.getMessage());
        }
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;

        log.info("Starting config auto-registration to governance center...");

        String serviceName = properties.getServiceName();
        String environmentName = getEnvironmentName();
        List<String> prefixes = properties.getEffectiveAutoRegisterPrefixes();
        int success = 0;
        int failed = 0;

        log.info("Registering configs for service: {}, environment: {}, prefixes: {}",
                serviceName, environmentName, prefixes);

        int totalMetadata = metadataCache.size();
        int filtered = 0;

        for (Map.Entry<String, ConfigMetadata> entry : metadataCache.entrySet()) {
            String key = entry.getKey();
            ConfigMetadata metadata = entry.getValue();

            if (!shouldRegister(key, prefixes)) {
                filtered++;
                continue;
            }

            if (metadata.deprecated) {
                log.debug("Skipping deprecated config: {}", key);
                continue;
            }

            String currentValue = environment.getProperty(key);

            Object valueToReport = currentValue;
            if (currentValue == null && metadata.defaultValue != null) {
                valueToReport = metadata.defaultValue;
            }

            String displayName = getDisplayName(key);
            String valueStr = convertValueToString(valueToReport, metadata.type);
            String defaultValueStr = metadata.defaultValue != null
                    ? String.valueOf(metadata.defaultValue) : null;

            boolean result = configClient.publishConfig(
                    key, valueStr, "auto-registration", "system",
                    serviceName, environmentName, displayName,
                    metadata.type, defaultValueStr, metadata.deprecated);

            if (result) {
                success++;
                log.debug("Auto-registered config: {} = {} (current), default: {}, type: {}",
                        key, valueStr, defaultValueStr, metadata.type);
            } else {
                failed++;
                log.warn("Failed to auto-register config: {}", key);
            }
        }

        log.info("Config auto-registration completed: {} total metadata, {} filtered by prefix, {} success, {} failed",
                totalMetadata, filtered, success, failed);
    }

    private String getEnvironmentName() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            String profile = activeProfiles[0];
            if (profile.contains("prod") || profile.contains("production")) {
                return "prod";
            } else if (profile.contains("test") || profile.contains("testing")) {
                return "test";
            } else {
                return "dev";
            }
        }
        return "dev";
    }

    private String getDisplayName(String key) {
        Map<String, String> externalDisplayNames = properties.getDisplayNames();
        if (externalDisplayNames != null && externalDisplayNames.containsKey(key)) {
            return externalDisplayNames.get(key);
        }
        if (metadataDescriptions.containsKey(key)) {
            return metadataDescriptions.get(key);
        }
        return key;
    }

    private boolean shouldRegister(String prefix, List<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return true;
        }
        for (String filter : prefixes) {
            if (prefix.startsWith(filter)) {
                return true;
            }
        }
        return false;
    }

    private String convertValueToString(Object value, String metaType) {
        if (value == null) {
            return "";
        }

        if (metaType != null) {
            if (metaType.equals("java.lang.Boolean") || metaType.equals("boolean")) {
                if (value instanceof Boolean) {
                    return value.toString();
                }
                if (value instanceof String) {
                    return Boolean.toString(Boolean.parseBoolean((String) value));
                }
            }

            if (metaType.startsWith("java.lang.Number") ||
                    metaType.equals("java.lang.Integer") || metaType.equals("int") ||
                    metaType.equals("java.lang.Long") || metaType.equals("long") ||
                    metaType.equals("java.lang.Double") || metaType.equals("double") ||
                    metaType.equals("java.lang.Float") || metaType.equals("float")) {
                if (value instanceof Number) {
                    return value.toString();
                }
            }

            if (metaType.startsWith("java.util.List") || metaType.startsWith("java.util.Map")) {
                try {
                    return objectMapper.writeValueAsString(value);
                } catch (Exception e) {
                    return value.toString();
                }
            }
        }

        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
