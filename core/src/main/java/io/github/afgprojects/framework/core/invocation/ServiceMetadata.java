package io.github.afgprojects.framework.core.invocation;

import java.util.List;

public interface ServiceMetadata<T> {
    String serviceName();
    String description();
    String category();
    List<String> tags();
    Class<T> serviceType();
    List<OperationMetadata> operations();
}
