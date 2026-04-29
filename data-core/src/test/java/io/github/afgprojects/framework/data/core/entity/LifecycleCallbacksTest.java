package io.github.afgprojects.framework.data.core.entity;

import io.github.afgprojects.framework.data.core.context.AuditContext;
import io.github.afgprojects.framework.data.core.context.EntityContext;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * LifecycleCallbacks 接口测试
 */
@DisplayName("LifecycleCallbacks 接口测试")
class LifecycleCallbacksTest {

    @Nested
    @DisplayName("默认方法测试")
    class DefaultMethodTests {

        @Test
        @DisplayName("默认 beforeCreate 方法应该无操作")
        void defaultBeforeCreateShouldDoNothing() {
            LifecycleCallbacks callbacks = new TestLifecycleCallbacks();
            EntityContext context = createMockContext();
            assertThatCode(() -> callbacks.beforeCreate(context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("默认 afterCreate 方法应该无操作")
        void defaultAfterCreateShouldDoNothing() {
            LifecycleCallbacks callbacks = new TestLifecycleCallbacks();
            EntityContext context = createMockContext();
            assertThatCode(() -> callbacks.afterCreate(context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("默认 beforeUpdate 方法应该无操作")
        void defaultBeforeUpdateShouldDoNothing() {
            LifecycleCallbacks callbacks = new TestLifecycleCallbacks();
            EntityContext context = createMockContext();
            assertThatCode(() -> callbacks.beforeUpdate(context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("默认 afterUpdate 方法应该无操作")
        void defaultAfterUpdateShouldDoNothing() {
            LifecycleCallbacks callbacks = new TestLifecycleCallbacks();
            EntityContext context = createMockContext();
            assertThatCode(() -> callbacks.afterUpdate(context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("默认 beforeDelete 方法应该无操作")
        void defaultBeforeDeleteShouldDoNothing() {
            LifecycleCallbacks callbacks = new TestLifecycleCallbacks();
            EntityContext context = createMockContext();
            assertThatCode(() -> callbacks.beforeDelete(context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("默认 afterDelete 方法应该无操作")
        void defaultAfterDeleteShouldDoNothing() {
            LifecycleCallbacks callbacks = new TestLifecycleCallbacks();
            EntityContext context = createMockContext();
            assertThatCode(() -> callbacks.afterDelete(context)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("自定义回调实现测试")
    class CustomCallbackTests {

        @Test
        @DisplayName("beforeCreate 回调应该正确执行")
        void beforeCreateCallbackShouldExecute() {
            StringBuilder log = new StringBuilder();
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeCreate(EntityContext context) {
                    log.append("beforeCreate;");
                }
            };
            EntityContext context = createMockContext();
            callbacks.beforeCreate(context);
            assertThat(log.toString()).isEqualTo("beforeCreate;");
        }

        @Test
        @DisplayName("所有回调应该按顺序执行")
        void allCallbacksShouldExecuteInOrder() {
            StringBuilder log = new StringBuilder();
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeCreate(EntityContext ctx) { log.append("beforeCreate;"); }
                @Override
                public void afterCreate(EntityContext ctx) { log.append("afterCreate;"); }
                @Override
                public void beforeUpdate(EntityContext ctx) { log.append("beforeUpdate;"); }
                @Override
                public void afterUpdate(EntityContext ctx) { log.append("afterUpdate;"); }
                @Override
                public void beforeDelete(EntityContext ctx) { log.append("beforeDelete;"); }
                @Override
                public void afterDelete(EntityContext ctx) { log.append("afterDelete;"); }
            };
            EntityContext context = createMockContext();
            callbacks.beforeCreate(context);
            callbacks.afterCreate(context);
            callbacks.beforeUpdate(context);
            callbacks.afterUpdate(context);
            callbacks.beforeDelete(context);
            callbacks.afterDelete(context);
            assertThat(log.toString()).isEqualTo("beforeCreate;afterCreate;beforeUpdate;afterUpdate;beforeDelete;afterDelete;");
        }
    }

    @Nested
    @DisplayName("EntityContext 交互测试")
    class ContextInteractionTests {

        @Test
        @DisplayName("回调应该能访问实体对象")
        void callbackShouldAccessEntity() {
            TestEntity entity = new TestEntity();
            entity.setId(42L);
            EntityContext context = createMockContextWithEntity(entity);
            final Object[] capturedEntity = new Object[1];
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeCreate(EntityContext ctx) {
                    capturedEntity[0] = ctx.getEntity();
                }
            };
            callbacks.beforeCreate(context);
            assertThat(capturedEntity[0]).isEqualTo(entity);
        }

        @Test
        @DisplayName("回调应该能修改实体属性")
        void callbackShouldModifyEntity() {
            TestEntity entity = new TestEntity();
            EntityContext context = createMockContextWithEntity(entity);
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeCreate(EntityContext ctx) {
                    TestEntity e = (TestEntity) ctx.getEntity();
                    e.setCreateTime(LocalDateTime.now());
                }
            };
            callbacks.beforeCreate(context);
            assertThat(entity.getCreateTime()).isNotNull();
        }

        @Test
        @DisplayName("回调应该能访问上下文属性")
        void callbackShouldAccessContextAttributes() {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("userId", "user123");
            EntityContext context = createMockContextWithAttributes(attributes);
            final Object[] userId = new Object[1];
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeCreate(EntityContext ctx) {
                    userId[0] = ctx.getAttribute("userId");
                }
            };
            callbacks.beforeCreate(context);
            assertThat(userId[0]).isEqualTo("user123");
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("回调抛出异常应该正确传播")
        void callbackExceptionShouldPropagate() {
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeCreate(EntityContext ctx) {
                    throw new RuntimeException("Callback failed");
                }
            };
            EntityContext context = createMockContext();
            assertThatCode(() -> callbacks.beforeCreate(context))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Callback failed");
        }

        @Test
        @DisplayName("回调可以抛出业务异常")
        void callbackCanThrowBusinessException() {
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeDelete(EntityContext ctx) {
                    throw new IllegalStateException("Cannot delete this entity");
                }
            };
            EntityContext context = createMockContext();
            assertThatCode(() -> callbacks.beforeDelete(context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot delete this entity");
        }
    }

    @Nested
    @DisplayName("CRUD 生命周期场景测试")
    class LifecycleScenarioTests {

        @Test
        @DisplayName("创建生命周期：beforeCreate -> afterCreate")
        void createLifecycle() {
            StringBuilder log = new StringBuilder();
            TestEntity entity = new TestEntity();
            EntityContext context = createMockContextWithEntity(entity);
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeCreate(EntityContext ctx) {
                    log.append("beforeCreate;");
                    ((TestEntity) ctx.getEntity()).setCreateTime(LocalDateTime.now());
                }
                @Override
                public void afterCreate(EntityContext ctx) { log.append("afterCreate;"); }
            };
            callbacks.beforeCreate(context);
            callbacks.afterCreate(context);
            assertThat(log.toString()).isEqualTo("beforeCreate;afterCreate;");
            assertThat(entity.getCreateTime()).isNotNull();
        }

        @Test
        @DisplayName("更新生命周期：beforeUpdate -> afterUpdate")
        void updateLifecycle() {
            StringBuilder log = new StringBuilder();
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            EntityContext context = createMockContextWithEntity(entity);
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeUpdate(EntityContext ctx) {
                    log.append("beforeUpdate;");
                    ((TestEntity) ctx.getEntity()).setUpdateTime(LocalDateTime.now());
                }
                @Override
                public void afterUpdate(EntityContext ctx) { log.append("afterUpdate;"); }
            };
            callbacks.beforeUpdate(context);
            callbacks.afterUpdate(context);
            assertThat(log.toString()).isEqualTo("beforeUpdate;afterUpdate;");
            assertThat(entity.getUpdateTime()).isNotNull();
        }

        @Test
        @DisplayName("删除生命周期：beforeDelete -> afterDelete")
        void deleteLifecycle() {
            StringBuilder log = new StringBuilder();
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            EntityContext context = createMockContextWithEntity(entity);
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeDelete(EntityContext ctx) { log.append("beforeDelete;"); }
                @Override
                public void afterDelete(EntityContext ctx) {
                    log.append("afterDelete;");
                    ((TestEntity) ctx.getEntity()).markDeleted();
                }
            };
            callbacks.beforeDelete(context);
            callbacks.afterDelete(context);
            assertThat(log.toString()).isEqualTo("beforeDelete;afterDelete;");
            assertThat(entity.isDeleted()).isTrue();
        }
    }

    private EntityContext createMockContext() {
        EntityContext context = mock(EntityContext.class);
        AuditContext auditContext = mock(AuditContext.class);
        when(auditContext.getCurrentTime()).thenReturn(Instant.now());
        when(context.getAuditContext()).thenReturn(auditContext);
        return context;
    }

    private EntityContext createMockContextWithEntity(Object entity) {
        EntityContext context = mock(EntityContext.class);
        when(context.getEntity()).thenReturn(entity);
        AuditContext auditContext = mock(AuditContext.class);
        when(auditContext.getCurrentTime()).thenReturn(Instant.now());
        when(context.getAuditContext()).thenReturn(auditContext);
        doReturn(null).when(context).getMetadata();
        return context;
    }

    private EntityContext createMockContextWithAttributes(Map<String, Object> attributes) {
        EntityContext context = mock(EntityContext.class);
        when(context.getAttribute("userId")).thenReturn(attributes.get("userId"));
        AuditContext auditContext = mock(AuditContext.class);
        when(auditContext.getCurrentTime()).thenReturn(Instant.now());
        when(context.getAuditContext()).thenReturn(auditContext);
        return context;
    }

    static class TestLifecycleCallbacks implements LifecycleCallbacks {}

    static class TestEntity extends SoftDeleteEntity<Long> {}
}
