package io.github.afgprojects.framework.core.invocation;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultServiceMetadataRegistry implements ServiceMetadataRegistry {

    private final ConcurrentHashMap<String, ServiceMetadata<?>> services = new ConcurrentHashMap<>();

    @Override
    public void register(ServiceMetadata<?> metadata) {
        services.put(metadata.serviceName(), metadata);
    }

    @Override
    public Optional<ServiceMetadata<?>> get(String serviceName) {
        return Optional.ofNullable(services.get(serviceName));
    }

    @Override
    public Optional<OperationMetadata> getOperation(String serviceName, String operationName) {
        return get(serviceName).flatMap(sm ->
                sm.operations().stream()
                        .filter(op -> op.name().equals(operationName))
                        .findFirst());
    }

    @Override
    public List<ServiceMetadata<?>> getAll() {
        return List.copyOf(services.values());
    }

    @Override
    public List<ServiceMetadata<?>> getByCategory(String category) {
        return services.values().stream()
                .filter(m -> category.equals(m.category()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceMetadata<?>> getByTag(String tag) {
        return services.values().stream()
                .filter(m -> m.tags().contains(tag))
                .collect(Collectors.toList());
    }
}
