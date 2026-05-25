package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CollectionResolver implements ArgumentResolver {

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public boolean supports(ParameterMetadata param, Object rawValue) {
        if (!(rawValue instanceof Collection<?>)) return false;
        String type = param.type();
        return type.equals("java.util.List") || type.equals("java.util.Set")
                || type.equals("java.util.Collection");
    }

    @Override
    public Object resolve(ResolveContext context) {
        Collection<?> raw = (Collection<?>) context.rawValue();
        String type = context.parameterMetadata().type();
        if (type.equals("java.util.Set") || type.equals("java.util.HashSet")) {
            return new HashSet<>(raw);
        }
        if (type.equals("java.util.LinkedHashSet")) {
            return new LinkedHashSet<>(raw);
        }
        return new ArrayList<>(raw);
    }
}