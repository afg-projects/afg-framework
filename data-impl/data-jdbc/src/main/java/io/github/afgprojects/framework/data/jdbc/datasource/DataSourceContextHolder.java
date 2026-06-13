/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.jdbc.datasource;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 数据源上下文持有者
 * <p>
 * 使用 ThreadLocal 存储当前线程的数据源名称，支持嵌套切换（栈结构）。
 * {@code DataSourceAspect} 在方法执行前 push 数据源名称，
 * 方法执行后 pop 恢复为之前的数据源。
 *
 * <p>此实现同时兼容框架自身的数据源路由与
 * {@code com.baomidou.dynamic.datasource} 的 {@code DynamicDataSourceContextHolder}，
 * 在 push/pop 时同步操作两边上下文，确保一致性。
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 编程式切换
 * DataSourceContextHolder.push("slave_1");
 * try {
 *     List&lt;User&gt; users = dataManager.findAll(User.class);
 * } finally {
 *     DataSourceContextHolder.pop();
 * }
 * </pre>
 *
 * @see io.github.afgprojects.framework.data.core.annotation.DataSource
 * @see DataSourceAspect
 */
public final class DataSourceContextHolder {

    private static final ThreadLocal<Deque<String>> CONTEXT = ThreadLocal.withInitial(ArrayDeque::new);

    private DataSourceContextHolder() {
        // 工具类不允许实例化
    }

    /**
     * 设置当前数据源名称（替换栈顶）
     *
     * @param dataSourceName 数据源名称，null 表示使用默认数据源
     */
    public static void set(@Nullable String dataSourceName) {
        Deque<String> stack = CONTEXT.get();
        if (dataSourceName == null) {
            if (!stack.isEmpty()) {
                stack.pop();
            }
        } else {
            stack.push(dataSourceName);
        }
        // 同步到 baomidou DynamicDataSourceContextHolder
        syncToDynamicDataSource(dataSourceName);
    }

    /**
     * 压入数据源名称（栈操作）
     *
     * @param dataSourceName 数据源名称
     */
    public static void push(@Nullable String dataSourceName) {
        if (dataSourceName != null) {
            CONTEXT.get().push(dataSourceName);
            syncToDynamicDataSource(dataSourceName);
        }
    }

    /**
     * 弹出数据源名称（栈操作，恢复到上一层）
     */
    public static void pop() {
        Deque<String> stack = CONTEXT.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        // 恢复到栈顶数据源
        String current = peek();
        syncToDynamicDataSource(current);
    }

    /**
     * 获取当前数据源名称
     *
     * @return 当前数据源名称，null 表示使用默认数据源
     */
    public static @Nullable String get() {
        return peek();
    }

    /**
     * 查看栈顶数据源名称（不弹出）
     *
     * @return 栈顶数据源名称，null 表示使用默认数据源
     */
    public static @Nullable String peek() {
        Deque<String> stack = CONTEXT.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    /**
     * 清除当前线程的所有数据源上下文
     */
    public static void clear() {
        CONTEXT.remove();
        syncToDynamicDataSource(null);
    }

    /**
     * 判断当前是否指定了非默认数据源
     *
     * @return 如果指定了数据源名称返回 true
     */
    public static boolean isSpecified() {
        return peek() != null;
    }

    // ==================== 与 baomidou DynamicDataSource 同步 ====================

    /**
     * 同步数据源名称到 baomidou DynamicDataSourceContextHolder
     * <p>
     * 如果 classpath 上存在 baomidou dynamic-datasource，则同步操作其上下文。
     * 如果不存在，则忽略（反射安全检测）。
     *
     * @param dataSourceName 数据源名称
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    private static void syncToDynamicDataSource(@Nullable String dataSourceName) {
        try {
            Class<?> clazz = Class.forName(
                    "com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder");
            if (dataSourceName != null) {
                var method = clazz.getMethod("push", String.class);
                method.invoke(null, dataSourceName);
            } else {
                var method = clazz.getMethod("poll");
                method.invoke(null);
            }
        } catch (ClassNotFoundException | NoSuchMethodException
                 | java.lang.reflect.InvocationTargetException | IllegalAccessException e) {
            // baomidou dynamic-datasource not available, method signature changed, or not accessible
        }
    }
}
