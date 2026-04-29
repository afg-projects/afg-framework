package io.github.afgprojects.framework.core.support;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

/**
 * 单元测试基类
 * 提供 Mock 对象创建和测试实例构造能力
 */
public abstract class BaseUnitTest {

    @BeforeEach
    protected void setUpBase() {
        // 子类可覆盖进行初始化
    }

    @AfterEach
    protected void tearDownBase() {
        // 子类可覆盖进行清理
    }

    /**
     * 创建 Mock 对象
     *
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return Mock 对象
     */
    protected <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz);
    }

    /**
     * 创建真实对象实例
     *
     * @param clazz 目标类型
     * @param args  构造参数
     * @param <T>   类型参数
     * @return 实例对象
     */
    protected <T> T createInstance(Class<T> clazz, Object... args) {
        try {
            @SuppressWarnings("unchecked")
            Constructor<T> constructor = (Constructor<T>) clazz.getDeclaredConstructor(
                    Arrays.stream(args).map(Object::getClass).toArray(Class[]::new));
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance: " + clazz.getName(), e);
        }
    }
}
