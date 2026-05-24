package io.github.afgprojects.framework.core.invocation;

import java.util.List;

public interface ParameterMetadata {
    String name();
    String type();
    boolean required();
    String defaultValue();
    int index();
    String description();
    List<String> enumValues();
    boolean injected();
}
