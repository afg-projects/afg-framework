package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.core.invocation.BeanInvocationEngine;
import io.github.afgprojects.framework.core.invocation.OperationMetadata;
import io.github.afgprojects.framework.core.invocation.ServiceMetadata;
import io.github.afgprojects.framework.core.invocation.ServiceMetadataRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Optional;

/**
 * Scans the {@link ServiceMetadataRegistry} at application startup and
 * auto-registers each non-deprecated {@link OperationMetadata} as a
 * {@link ServiceToolAdapter} in the {@link ToolRegistry}.
 *
 * <p>Manually defined tools take precedence — if a tool with the same name
 * already exists in the registry, the auto-registration is skipped.
 *
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ServiceToolRegistrar implements ApplicationRunner {

    private final ServiceMetadataRegistry metadataRegistry;
    private final BeanInvocationEngine engine;
    private final ToolRegistry toolRegistry;

    @Override
    public void run(ApplicationArguments args) {
        for (ServiceMetadata<?> sm : metadataRegistry.getAll()) {
            for (OperationMetadata op : sm.operations()) {
                if (op.deprecated()) continue;

                String toolName = sm.serviceName() + "." + op.name();

                if (toolRegistry.exists(toolName)) {
                    log.info("Skipping auto-registered tool '{}': manually defined tool takes precedence", toolName);
                    continue;
                }

                ServiceToolAdapter adapter = new ServiceToolAdapter(sm.serviceName(), op, engine);
                toolRegistry.register(adapter);
                log.info("Auto-registered tool: {}", toolName);
            }
        }
    }
}
