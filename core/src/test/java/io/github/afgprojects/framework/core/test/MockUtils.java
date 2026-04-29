package io.github.afgprojects.framework.core.test;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.mockito.Mockito;

/**
 * Mock 工具类
 * 提供简化的 Mock 对象创建和配置方法
 *
 * <p>使用示例:
 * <pre>{@code
 * // 创建 Mock 对象
 * UserService userService = MockUtils.mock(UserService.class);
 *
 * // 创建并配置 Mock
 * UserService userService = MockUtils.mock(UserService.class, m -> {
 *     when(m.findById("001")).thenReturn(user);
 *     when(m.save(any())).thenReturn(user);
 * });
 *
 * // 创建松散 Mock（不抛出异常）
 * UserService userService = MockUtils.lenientMock(UserService.class);
 *
 * // 创建 Spy
 * UserService userService = MockUtils.spy(realService);
 * }</pre>
 */
public final class MockUtils {

    private MockUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 创建 Mock 对象
     *
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return Mock 对象
     */
    public static <T> T mock(@NonNull Class<T> clazz) {
        return Mockito.mock(clazz);
    }

    /**
     * 创建 Mock 对象并配置
     *
     * @param clazz       目标类型
     * @param configurator 配置回调
     * @param <T>         类型参数
     * @return Mock 对象
     */
    public static <T> T mock(@NonNull Class<T> clazz, @NonNull Function<T, T> configurator) {
        T mock = Mockito.mock(clazz);
        configurator.apply(mock);
        return mock;
    }

    /**
     * 创建松散 Mock 对象
     * 未配置的方法调用不会抛出异常，返回默认值
     *
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return Mock 对象
     */
    public static <T> T lenientMock(@NonNull Class<T> clazz) {
        return Mockito.mock(clazz, Mockito.withSettings().lenient());
    }

    /**
     * 创建松散 Mock 对象并配置
     *
     * @param clazz       目标类型
     * @param configurator 配置回调
     * @param <T>         类型参数
     * @return Mock 对象
     */
    public static <T> T lenientMock(@NonNull Class<T> clazz, @NonNull Function<T, T> configurator) {
        T mock = Mockito.mock(clazz, Mockito.withSettings().lenient());
        configurator.apply(mock);
        return mock;
    }

    /**
     * 创建 Spy 对象
     * Spy 会调用真实方法，除非被显式 Mock
     *
     * @param object 真实对象
     * @param <T>    类型参数
     * @return Spy 对象
     */
    public static <T> T spy(@NonNull T object) {
        return Mockito.spy(object);
    }

    /**
     * 创建 Spy 对象并配置
     *
     * @param object      真实对象
     * @param configurator 配置回调
     * @param <T>         类型参数
     * @return Spy 对象
     */
    public static <T> T spy(@NonNull T object, @NonNull Function<T, T> configurator) {
        T spy = Mockito.spy(object);
        configurator.apply(spy);
        return spy;
    }

    /**
     * 创建 Spy（使用类而非实例）
     * 注意：此方法会尝试创建实例，需要无参构造器
     *
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return Spy 对象
     */
    public static <T> T spy(@NonNull Class<T> clazz) {
        return Mockito.spy(clazz);
    }

    /**
     * 配置 Mock 返回值
     *
     * @param mock  Mock 对象
     * @param value 返回值
     * @param <T>   类型参数
     * @return Mock 对象
     */
    public static <T> T whenReturn(@NonNull T mock, @Nullable T value) {
        when(mock).thenReturn(value);
        return mock;
    }

    /**
     * 配置 Mock 抛出异常
     *
     * @param mock     Mock 对象
     * @param exception 异常
     * @param <T>      类型参数
     * @return Mock 对象
     */
    public static <T> T whenThrow(@NonNull T mock, @NonNull Throwable exception) {
        when(mock).thenThrow(exception);
        return mock;
    }

    /**
     * 重置 Mock 对象
     *
     * @param mocks Mock 对象
     */
    public static void reset(@NonNull Object... mocks) {
        Mockito.reset(mocks);
    }

    /**
     * 验证 Mock 方法调用次数
     *
     * @param mock  Mock 对象
     * @param times 调用次数
     * @param <T>   类型参数
     * @return 用于进一步验证的对象
     */
    public static <T> T verify(@NonNull T mock, int times) {
        return Mockito.verify(mock, Mockito.times(times));
    }

    /**
     * 验证 Mock 方法从未被调用
     *
     * @param mock Mock 对象
     * @param <T>  类型参数
     * @return 用于进一步验证的对象
     */
    public static <T> T verifyNever(@NonNull T mock) {
        return Mockito.verify(mock, Mockito.never());
    }

    /**
     * 验证 Mock 方法被调用至少 n 次
     *
     * @param mock  Mock 对象
     * @param times 最少调用次数
     * @param <T>   类型参数
     * @return 用于进一步验证的对象
     */
    public static <T> T verifyAtLeast(@NonNull T mock, int times) {
        return Mockito.verify(mock, Mockito.atLeast(times));
    }

    /**
     * 验证 Mock 方法被调用最多 n 次
     *
     * @param mock  Mock 对象
     * @param times 最多调用次数
     * @param <T>   类型参数
     * @return 用于进一步验证的对象
     */
    public static <T> T verifyAtMost(@NonNull T mock, int times) {
        return Mockito.verify(mock, Mockito.atMost(times));
    }

    /**
     * 验证 Mock 方法被调用一次
     *
     * @param mock Mock 对象
     * @param <T>  类型参数
     * @return 用于进一步验证的对象
     */
    public static <T> T verifyOnce(@NonNull T mock) {
        return Mockito.verify(mock, Mockito.times(1));
    }

    /**
     * 验证没有更多交互
     *
     * @param mocks Mock 对象
     */
    public static void verifyNoMoreInteractions(@NonNull Object... mocks) {
        Mockito.verifyNoMoreInteractions(mocks);
    }

    /**
     * 验证没有交互
     *
     * @param mocks Mock 对象
     */
    public static void verifyNoInteractions(@NonNull Object... mocks) {
        Mockito.verifyNoInteractions(mocks);
    }

    /**
     * 配置 Mock 的 toString 方法
     *
     * @param mock Mock 对象
     * @param name 名称
     * @param <T>  类型参数
     * @return Mock 对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T withName(@NonNull T mock, @NonNull String name) {
        return (T) Mockito.mock(mock.getClass(), Mockito.withSettings().name(name));
    }

    /**
     * 检查是否是 Mock 对象
     *
     * @param object 对象
     * @return 是否是 Mock
     */
    public static boolean isMock(@Nullable Object object) {
        return Mockito.mockingDetails(object).isMock();
    }

    /**
     * 检查是否是 Spy 对象
     *
     * @param object 对象
     * @return 是否是 Spy
     */
    public static boolean isSpy(@Nullable Object object) {
        return Mockito.mockingDetails(object).isSpy();
    }

    /**
     * 获取 Mock 的调用次数
     *
     * @param mock Mock 对象
     * @return 调用次数
     */
    public static int getInvocationCount(@NonNull Object mock) {
        return Mockito.mockingDetails(mock).getInvocations().size();
    }

    /**
     * 清除 Mock 的调用记录（不重置配置）
     *
     * @param mocks Mock 对象
     */
    public static void clearInvocations(@NonNull Object... mocks) {
        Mockito.clearInvocations(mocks);
    }

    // ==================== 常见 Mock 配置 ====================

    /**
     * Mock Iterable/Collection 的迭代器
     *
     * @param iterable Mock 的 Iterable 对象
     * @param elements 元素列表
     * @param <T>      元素类型
     * @return Mock 对象
     */
    @SuppressWarnings("unchecked")
    public static <T> Iterable<T> mockIterable(@NonNull Iterable<T> iterable, @NonNull T... elements) {
        when(iterable.iterator()).thenReturn(MockUtils.mockIterator(elements));
        return iterable;
    }

    /**
     * 创建 Mock Iterator
     *
     * @param elements 元素列表
     * @param <T>      元素类型
     * @return Mock Iterator
     */
    @SafeVarargs
    public static <T> java.util.Iterator<T> mockIterator(@NonNull T... elements) {
        java.util.Iterator<T> iterator = mock(java.util.Iterator.class);
        if (elements.length == 0) {
            when(iterator.hasNext()).thenReturn(false);
        } else {
            // 配置 hasNext 返回值
            Boolean[] hasNextValues = new Boolean[elements.length];
            for (int i = 0; i < elements.length; i++) {
                hasNextValues[i] = i < elements.length - 1;
            }
            when(iterator.hasNext()).thenReturn(true, hasNextValues);

            // 配置 next 返回值
            @SuppressWarnings("unchecked")
            T[] nextValues = (T[]) java.lang.reflect.Array.newInstance(
                    elements.getClass().getComponentType(), elements.length);
            System.arraycopy(elements, 0, nextValues, 0, elements.length);
            when(iterator.next()).thenReturn(nextValues[0], java.util.Arrays.copyOfRange(nextValues, 1, nextValues.length));
        }
        return iterator;
    }

    /**
     * Mock Map 的 get 方法返回默认值
     *
     * @param map         Mock 的 Map 对象
     * @param defaultValue 默认值
     * @param <K>         键类型
     * @param <V>         值类型
     * @return Mock 对象
     */
    public static <K, V> java.util.Map<K, V> mockMapWithDefault(
            java.util.Map<K, V> map, @Nullable V defaultValue) {
        lenient().when(map.get(Mockito.any())).thenReturn(defaultValue);
        return map;
    }
}