package io.github.afgprojects.framework.core.security.datascope;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * 数据权限辅助工具类
 * <p>
 * 提供 @DataScope 注解的解析、SQL 条件生成等辅助方法
 */
public final class DataScopeHelper {

    private DataScopeHelper() {
        // 私有构造函数，防止实例化
    }

    /**
     * 从方法上获取所有 @DataScope 注解
     * <p>
     * 优先从方法上获取，如果没有则从类上获取
     *
     * @param method 方法
     * @param targetClass 目标类
     * @return @DataScope 注解列表
     */
    public static List<DataScope> getDataScopes(Method method, @Nullable Class<?> targetClass) {
        List<DataScope> dataScopes = new ArrayList<>();

        // 优先从方法上获取
        DataScope methodAnnotation = AnnotationUtils.findAnnotation(method, DataScope.class);
        if (methodAnnotation != null) {
            dataScopes.add(methodAnnotation);
        }

        // 检查方法上的 @DataScope.List
        DataScope.List methodList = AnnotationUtils.findAnnotation(method, DataScope.List.class);
        if (methodList != null) {
            dataScopes.addAll(Arrays.asList(methodList.value()));
        }

        // 如果方法上没有，从类上获取
        if (dataScopes.isEmpty() && targetClass != null) {
            DataScope classAnnotation = AnnotationUtils.findAnnotation(targetClass, DataScope.class);
            if (classAnnotation != null) {
                dataScopes.add(classAnnotation);
            }

            DataScope.List classList = AnnotationUtils.findAnnotation(targetClass, DataScope.List.class);
            if (classList != null) {
                dataScopes.addAll(Arrays.asList(classList.value()));
            }
        }

        return Collections.unmodifiableList(dataScopes);
    }

    /**
     * 生成数据权限 SQL 条件
     *
     * @param dataScope      @DataScope 注解
     * @param context        数据权限上下文
     * @param properties     配置属性
     * @return SQL 条件字符串，如果不需要过滤则返回 null
     */
    public static @Nullable String buildDataScopeCondition(
            DataScope dataScope,
            DataScopeContext context,
            DataScopeProperties properties) {

        // 如果忽略数据权限或拥有全部权限，不生成条件
        if (context.isIgnoreDataScope() || context.isAllDataPermission()) {
            return null;
        }

        DataScopeType scopeType = dataScope.scopeType();
        String table = resolveTableAlias(dataScope);
        String column = dataScope.column();

        return switch (scopeType) {
            case ALL -> null;
            case DEPT -> buildDeptCondition(table, column, context);
            case DEPT_AND_CHILD -> buildDeptAndChildCondition(dataScope, context, properties);
            case SELF -> buildSelfCondition(table, dataScope.userIdColumn(), context);
            case CUSTOM -> buildCustomCondition(dataScope, context);
        };
    }

    /**
     * 解析表名（支持别名）
     *
     * @param dataScope @DataScope 注解
     * @return 解析后的表名或别名
     */
    private static String resolveTableAlias(DataScope dataScope) {
        String table = dataScope.table();
        String aliasPrefix = dataScope.aliasPrefix();

        // 如果已指定别名前缀，使用它
        if (!aliasPrefix.isEmpty()) {
            return aliasPrefix;
        }

        // 如果表名包含空格，说明是 "表名 别名" 格式，提取别名
        int spaceIndex = table.indexOf(' ');
        if (spaceIndex > 0) {
            return table.substring(spaceIndex + 1).trim();
        }

        // 否则返回原表名
        return table;
    }

    /**
     * 生成本部门数据条件
     */
    private static String buildDeptCondition(String table, String column, DataScopeContext context) {
        Long deptId = context.getDeptId();
        if (deptId == null) {
            return null;
        }
        return String.format("%s.%s = %d", table, column, deptId);
    }

    /**
     * 生成本部门及子部门数据条件
     */
    private static @Nullable String buildDeptAndChildCondition(
            DataScope dataScope,
            DataScopeContext context,
            DataScopeProperties properties) {

        String table = resolveTableAlias(dataScope);
        String column = dataScope.column();
        Set<Long> deptIds = context.getAccessibleDeptIds();

        if (deptIds == null || deptIds.isEmpty()) {
            // 如果没有子部门，退化为仅本部门
            Long deptId = context.getDeptId();
            if (deptId == null) {
                return null;
            }
            return String.format("%s.%s = %d", table, column, deptId);
        }

        // 生成 IN 条件
        String deptIdStr = deptIds.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + ", " + b)
                .orElse("-1");

        return String.format("%s.%s IN (%s)", table, column, deptIdStr);
    }

    /**
     * 生成仅本人数据条件
     */
    private static @Nullable String buildSelfCondition(
            String table,
            String userIdColumn,
            DataScopeContext context) {

        Long userId = context.getUserId();
        if (userId == null) {
            return null;
        }
        return String.format("%s.%s = %d", table, userIdColumn, userId);
    }

    /**
     * 生成自定义条件
     */
    private static @Nullable String buildCustomCondition(DataScope dataScope, DataScopeContext context) {
        String customCondition = dataScope.customCondition();
        if (customCondition == null || customCondition.isEmpty()) {
            // 尝试使用上下文中的自定义条件
            return context.getCustomCondition();
        }
        return customCondition;
    }

    /**
     * 合并多个数据权限条件
     *
     * @param conditions 条件列表
     * @return 合并后的 WHERE 子句（不包含 WHERE 关键字）
     */
    public static @Nullable String mergeConditions(List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return null;
        }

        // 过滤掉 null 值
        List<String> validConditions = conditions.stream()
                .filter(c -> c != null && !c.isEmpty())
                .toList();

        if (validConditions.isEmpty()) {
            return null;
        }

        // 多个条件用 AND 连接
        return String.join(" AND ", validConditions);
    }

    /**
     * 判断表是否在忽略列表中
     *
     * @param tableName  表名
     * @param properties 配置属性
     * @return 是否忽略
     */
    public static boolean isIgnoredTable(String tableName, DataScopeProperties properties) {
        String[] ignoreTables = properties.getIgnoreTables();
        if (ignoreTables == null || ignoreTables.length == 0) {
            return false;
        }
        return Arrays.stream(ignoreTables)
                .anyMatch(ignore -> ignore.equalsIgnoreCase(tableName));
    }

    /**
     * 判断方法是否在忽略列表中
     *
     * @param methodName 方法名
     * @param properties  配置属性
     * @return 是否忽略
     */
    public static boolean isIgnoredMethod(String methodName, DataScopeProperties properties) {
        String[] ignoreMethods = properties.getIgnoreMethods();
        if (ignoreMethods == null || ignoreMethods.length == 0) {
            return false;
        }
        return Arrays.stream(ignoreMethods)
                .anyMatch(pattern -> matchMethodPattern(methodName, pattern));
    }

    /**
     * 匹配方法名模式
     *
     * @param methodName 方法名
     * @param pattern     模式（支持 * 通配符）
     * @return 是否匹配
     */
    private static boolean matchMethodPattern(String methodName, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        if (pattern.startsWith("*") && pattern.endsWith("*")) {
            return methodName.contains(pattern.substring(1, pattern.length() - 1));
        }
        if (pattern.startsWith("*")) {
            return methodName.endsWith(pattern.substring(1));
        }
        if (pattern.endsWith("*")) {
            return methodName.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return methodName.equals(pattern);
    }
}