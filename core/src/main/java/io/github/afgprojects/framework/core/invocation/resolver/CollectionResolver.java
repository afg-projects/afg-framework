package io.github.afgprojects.framework.core.invocation.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CollectionResolver implements ArgumentResolver {
    @Override
    public int priority() {
        return 4;
    }

    @Override
    public boolean supports(Class<?> sourceType, Class<?> targetType) {
        return Collection.class.isAssignableFrom(sourceType) && Collection.class.isAssignableFrom(targetType);
    }

    @Override
    public Object resolve(Object source, Class<?> targetType, ResolveContext context) {
        Collection<?> src = (Collection<?>) source;
        if (Set.class.isAssignableFrom(targetType)) {
            return new LinkedHashSet<>(src);
        }
        if (List.class.isAssignableFrom(targetType)) {
            return new ArrayList<>(src);
        }
        return source;
    }
}
