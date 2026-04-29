package io.github.afgprojects.framework.core.web.shutdown;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/**
 * Tests for ShutdownOrder annotation.
 */
class ShutdownOrderTest {

    @Test
    void shouldHaveDefaultPhase() throws NoSuchMethodException {
        Method method = TestBean.class.getMethod("defaultMethod");
        ShutdownOrder annotation = method.getAnnotation(ShutdownOrder.class);

        assertNotNull(annotation);
        assertEquals("cleanup", annotation.phase());
    }

    @Test
    void shouldHaveDefaultOrder() throws NoSuchMethodException {
        Method method = TestBean.class.getMethod("defaultMethod");
        ShutdownOrder annotation = method.getAnnotation(ShutdownOrder.class);

        assertNotNull(annotation);
        assertEquals(0, annotation.order());
    }

    @Test
    void shouldHaveCustomPhase() throws NoSuchMethodException {
        Method method = TestBean.class.getMethod("customPhaseMethod");
        ShutdownOrder annotation = method.getAnnotation(ShutdownOrder.class);

        assertNotNull(annotation);
        assertEquals("drain", annotation.phase());
    }

    @Test
    void shouldHaveCustomOrder() throws NoSuchMethodException {
        Method method = TestBean.class.getMethod("customOrderMethod");
        ShutdownOrder annotation = method.getAnnotation(ShutdownOrder.class);

        assertNotNull(annotation);
        assertEquals(100, annotation.order());
    }

    /**
     * Test bean with annotated methods.
     */
    static class TestBean {

        @ShutdownOrder
        public void defaultMethod() {}

        @ShutdownOrder(phase = "drain")
        public void customPhaseMethod() {}

        @ShutdownOrder(order = 100)
        public void customOrderMethod() {}
    }
}
