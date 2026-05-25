package io.github.afgprojects.framework.core.invocation.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.InvocationContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class SensitiveMaskProcessor implements ResultProcessor {
    private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "secret", "token", "credential");
    private static final String MASK = "***";

    @Override
    public int priority() {
        return 200;
    }

    @Override
    public boolean supports(InvocationContext context, Object result) {
        return result != null && hasSensitiveFields(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object process(ResultContext context) {
        Object result = context.result();
        try {
            ObjectMapper mapper = context.objectMapper();
            if (result instanceof List<?> list) {
                return list.stream().map(item -> maskItem(item, mapper)).toList();
            }
            return maskItem(result, mapper);
        } catch (Exception e) {
            log.warn("Failed to mask sensitive fields", e);
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private Object maskItem(Object item, ObjectMapper mapper) {
        if (item instanceof Map) {
            return maskMap((Map<String, Object>) item);
        }
        Map<String, Object> map = mapper.convertValue(item, Map.class);
        return maskMap(map);
    }

    private Map<String, Object> maskMap(Map<String, Object> map) {
        for (String key : SENSITIVE_FIELDS) {
            if (map.containsKey(key)) {
                map.put(key, MASK);
            }
        }
        return map;
    }

    private boolean hasSensitiveFields(Object result) {
        if (result instanceof Map<?, ?> map) {
            return map.keySet().stream().anyMatch(k -> SENSITIVE_FIELDS.contains(k.toString().toLowerCase()));
        }
        return false;
    }
}
