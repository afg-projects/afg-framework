package io.github.afgprojects.framework.data.jdbc.security.payload;

import java.util.List;
import java.util.stream.Stream;

/**
 * LIKE 注入 Payload 库
 */
public final class LikePayloads {

    private LikePayloads() {}

    public static final List<String> WILDCARD_INJECTION = List.of(
        "%' OR '1'='1",
        "%' OR 1=1--",
        "_%' OR '1'='1",
        "%' AND 1=1--"
    );

    public static final List<String> UNION_INJECTION = List.of(
        "%' UNION SELECT NULL--",
        "%' UNION SELECT username, password FROM users--",
        "%%' UNION ALL SELECT NULL--"
    );

    public static final List<String> ESCAPE_BYPASS = List.of(
        "%' ESCAPE '\\' OR '1'='1",
        "%\\' OR '1'='1",
        "%'/**/OR/**/'1'='1"
    );

    public static Stream<String> wildcardInjection() {
        return WILDCARD_INJECTION.stream();
    }

    public static Stream<String> unionInjection() {
        return UNION_INJECTION.stream();
    }

    public static Stream<String> escapeBypass() {
        return ESCAPE_BYPASS.stream();
    }

    public static Stream<String> all() {
        return Stream.of(WILDCARD_INJECTION, UNION_INJECTION, ESCAPE_BYPASS)
            .flatMap(List::stream);
    }
}
