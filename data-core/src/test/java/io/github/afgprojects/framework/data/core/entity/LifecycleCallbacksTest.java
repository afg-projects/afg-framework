package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
            assertThatCode(() -> callbacks.beforeCreate()).doesNotThrowAnyException();
        )

        @Test
        @DisplayName("默认 beforeUpdate 方法应该无操作")
        void defaultBeforeUpdateShouldDoNothing() {
            LifecycleCallbacks callbacks = new TestLifecycleCallbacks();
            assertThatCode(() -> callbacks.beforeUpdate()).doesNotThrowAnyException();
        )

        @Test
        @DisplayName("默认 afterLoad 方法应该无操作")
        void defaultAfterLoadShouldDoNothing() {
            LifecycleCallbacks callbacks = new TestLifecycleCallbacks();
            assertThatCode(() -> callbacks.afterLoad()).doesNotThrowAnyException();
        )

        @Test
        @DisplayName("默认 beforeDelete 方法应该无操作")
        void defaultBeforeDeleteShouldDoNothing() {
            LifecycleCallbacks callbacks = new TestLifecycleCallbacks();
            assertThatCode(() -> callbacks.beforeDelete()).doesNotThrowAnyException();
        )
    )

    @Nested
    @DisplayName("自定义回调实现测试")
    class CustomCallbackTests {

        @Test
        @DisplayName("beforeCreate 回调应该正确执行")
        void beforeCreateCallbackShouldExecute() {
            StringBuilder log = new StringBuilder();
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeCreate() {
                    log.append("beforeCreate;");
                )
            );
            callbacks.beforeCreate();
            assertThat(log.toString()).isEqualTo("beforeCreate;");
        )

        @Test
        @DisplayName("所有回调应该按顺序执行")
        void allCallbacksShouldExecuteInOrder() {
            StringBuilder log = new StringBuilder();
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeCreate() { log.append("beforeCreate;"); )
                @Override
                public void beforeUpdate() { log.append("beforeUpdate;"); )
                @Override
                public void afterLoad() { log.append("afterLoad;"); )
                @Override
                public void beforeDelete() { log.append("beforeDelete;"); )
            );
            callbacks.beforeCreate();
            callbacks.beforeUpdate();
            callbacks.afterLoad();
            callbacks.beforeDelete();
            assertThat(log.toString()).isEqualTo("beforeCreate;beforeUpdate;afterLoad;beforeDelete;");
        )
    )

    @Nested
    @DisplayName("ifCallback 静态方法测试")
    class IfCallbackTests {

        @Test
        @DisplayName("实体实现 LifecycleCallbacks 时应该执行回调")
        void shouldExecuteCallbackWhenEntityImplementsInterface() {
            StringBuilder log = new StringBuilder();
            TestEntity entity = new TestEntity(log);

            LifecycleCallbacks.ifCallback(entity, LifecycleCallbacks::beforeCreate);

            assertThat(log.toString()).isEqualTo("beforeCreate;");
        )

        @Test
        @DisplayName("实体未实现 LifecycleCallbacks 时不应该执行回调")
        void shouldNotExecuteCallbackWhenEntityDoesNotImplementInterface() {
            StringBuilder log = new StringBuilder();
            Object entity = new Object();

            LifecycleCallbacks.ifCallback(entity, cb -> log.append("called;"));

            assertThat(log.toString()).isEmpty();
        )

        @Test
        @DisplayName("实体为 null 时不应该执行回调")
        void shouldNotExecuteCallbackWhenEntityIsNull() {
            StringBuilder log = new StringBuilder();
            Object entity = null;

            LifecycleCallbacks.ifCallback(entity, cb -> log.append("called;"));

            assertThat(log.toString()).isEmpty();
        )
    )

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("回调抛出异常应该正确传播")
        void callbackExceptionShouldPropagate() {
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeCreate() {
                    throw new RuntimeException("Callback failed");
                )
            );
            assertThatCode(() -> callbacks.beforeCreate())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Callback failed");
        )

        @Test
        @DisplayName("回调可以抛出业务异常")
        void callbackCanThrowBusinessException() {
            LifecycleCallbacks callbacks = new LifecycleCallbacks() {
                @Override
                public void beforeDelete() {
                    throw new IllegalStateException("Cannot delete this entity");
                )
            );
            assertThatCode(() -> callbacks.beforeDelete())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot delete this entity");
        )
    )

    static class TestLifecycleCallbacks implements LifecycleCallbacks {)

    static class TestEntity extends SoftDeleteEntity implements LifecycleCallbacks {
        private final StringBuilder log;

        TestEntity(StringBuilder log) {
            this.log = log;
        )

        @Override
        public void beforeCreate() {
            log.append("beforeCreate;");
        )

        @Override
        public void beforeUpdate() {
            log.append("beforeUpdate;");
        )

        @Override
        public void afterLoad() {
            log.append("afterLoad;");
        )

        @Override
        public void beforeDelete() {
            log.append("beforeDelete;");
        )
    )
)