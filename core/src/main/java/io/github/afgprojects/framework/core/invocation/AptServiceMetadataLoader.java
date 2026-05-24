package io.github.afgprojects.framework.core.invocation;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

@Slf4j
public class AptServiceMetadataLoader {

    private static final String INDEX_LOCATION = "META-INF/afg/service-metadata.index";

    private final ServiceMetadataRegistry registry;

    public AptServiceMetadataLoader(ServiceMetadataRegistry registry) {
        this.registry = registry;
    }

    public void load() {
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(INDEX_LOCATION);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                loadIndex(url);
            }
        } catch (Exception e) {
            log.warn("Failed to load service metadata from APT index", e);
        }
    }

    private void loadIndex(URL indexUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(indexUrl.openStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String className;
            while ((className = reader.readLine()) != null) {
                className = className.trim();
                if (className.isEmpty() || className.startsWith("#")) continue;
                loadMetadata(className);
            }
        } catch (Exception e) {
            log.warn("Failed to read service metadata index from {}", indexUrl, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadMetadata(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (ServiceMetadata.class.isAssignableFrom(clazz)) {
                ServiceMetadata<?> metadata = (ServiceMetadata<?>) clazz.getDeclaredConstructor().newInstance();
                registry.register(metadata);
                log.info("Loaded service metadata: {}", metadata.serviceName());
            }
        } catch (Exception e) {
            log.warn("Failed to load service metadata class: {}", className, e);
        }
    }
}
