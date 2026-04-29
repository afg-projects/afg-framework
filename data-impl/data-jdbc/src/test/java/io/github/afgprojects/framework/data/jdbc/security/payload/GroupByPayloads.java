package io.github.afgprojects.framework.data.jdbc.security.payload;

import java.util.List;
import java.util.stream.Stream;

/**
 * GROUP BY / HAVING 注入 Payload 库
 */
public final class GroupByPayloads {

    private GroupByPayloads() {}

    public static final List<String> GROUP_BY_INJECTION = List.of(
        "name HAVING 1=1--",
        "name, id HAVING 1=1--",
        "name; DROP TABLE users--"
    );

    public static final List<String> HAVING_INJECTION = List.of(
        "name HAVING SUM(1) > 0 UNION SELECT NULL--",
        "name HAVING COUNT(*) > 0 OR 1=1--",
        "name HAVING (SELECT CASE WHEN (1=1) THEN 1 ELSE 1/0 END)"
    );

    public static Stream<String> groupByInjection() {
        return GROUP_BY_INJECTION.stream();
    }

    public static Stream<String> havingInjection() {
        return HAVING_INJECTION.stream();
    }

    public static Stream<String> all() {
        return Stream.of(GROUP_BY_INJECTION, HAVING_INJECTION)
            .flatMap(List::stream);
    }
}
