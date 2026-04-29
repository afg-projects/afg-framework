package io.github.afgprojects.framework.core.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import io.github.afgprojects.framework.core.event.ModuleEvent;

class ModuleContextTest {

    private ModuleContext moduleContext;
    private ModuleRegistry moduleRegistry;
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        moduleRegistry = mock(ModuleRegistry.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        moduleContext = new ModuleContext(moduleRegistry, eventPublisher);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ModuleContext instance")
        void shouldCreateInstance() {
            ModuleContext context = new ModuleContext(moduleRegistry, eventPublisher);

            assertThat(context).isNotNull();
            assertThat(context.getRegistry()).isEqualTo(moduleRegistry);
        }
    }

    @Nested
    @DisplayName("publishEvent Tests")
    class PublishEventTests {

        @Test
        @DisplayName("Should publish event to Spring event publisher")
        void shouldPublishEventToSpringPublisher() {
            ModuleEvent event = new ModuleEvent("test-module", "TEST_EVENT");

            moduleContext.publishEvent(event);

            verify(eventPublisher).publishEvent(event);
        }

        @Test
        @DisplayName("Should handle null event gracefully")
        void shouldHandleNullEvent() {
            moduleContext.publishEvent(null);

            verifyNoInteractions(eventPublisher);
        }
    }
}
