package io.github.afgprojects.framework.core.invocation;

import java.util.List;
import java.util.Optional;

public interface ServiceMetadataRegistry {
    void register(ServiceMetadata<?> metadata);
    Optional<ServiceMetadata<?>> get(String serviceName);
    Optional<OperationMetadata> getOperation(String serviceName, String operationName);
    List<ServiceMetadata<?>> getAll();
    List<ServiceMetadata<?>> getByCategory(String category);
    List<ServiceMetadata<?>> getByTag(String tag);
}
