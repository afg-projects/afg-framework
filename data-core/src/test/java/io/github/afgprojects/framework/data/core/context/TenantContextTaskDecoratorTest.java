/*
 * Copyright 2024 AFG Projects.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TenantContextTaskDecorator 单元测试
 * <p>
 * 测试跨线程租户上下文传播功能。
 */
class TenantContextTaskDecoratorTest {

    private TenantContextHolder holder;
    private TenantContextTaskDecorator decorator;

    @BeforeEach
    void setUp() {
        holder = new TenantContextHolder();
        decorator = new TenantContextTaskDecorator(holder);
    }

    @AfterEach
    void tearDown() {
        holder.clear();
    }

    @Nested
    @DisplayName("跨线程租户上下文传播")
    class CrossThreadPropagation {

        @Test
        @DisplayName("should propagate tenant context when decorate runnable")
        void shouldPropagateTenantContext_whenDecorateRunnable() throws InterruptedException {
            holder.setTenantId("tenant-001");

            Runnable originalTask = () -> {
                assertThat(holder.getTenantId()).isEqualTo("tenant-001");
            };

            Runnable decoratedTask = decorator.decorate(originalTask);

            // 在新线程中执行
            Thread thread = new Thread(decoratedTask);
            thread.start();
            thread.join(1000);
        }

        @Test
        @DisplayName("should not set tenant context when no tenant in parent thread")
        void shouldNotSetTenantContext_whenNoTenantInParentThread() throws InterruptedException {
            // 父线程没有设置租户
            assertThat(holder.getTenantId()).isNull();

            Runnable originalTask = () -> {
                assertThat(holder.getTenantId()).isNull();
            };

            Runnable decoratedTask = decorator.decorate(originalTask);

            Thread thread = new Thread(decoratedTask);
            thread.start();
            thread.join(1000);
        }

        @Test
        @DisplayName("should restore child context after decorated runnable runs")
        void shouldRestoreChildContext_afterDecoratedRunnableRuns() throws InterruptedException {
            holder.setTenantId("tenant-001");

            StringBuilder capturedBefore = new StringBuilder();
            StringBuilder capturedAfter = new StringBuilder();

            Runnable originalTask = () -> {
                capturedBefore.append(holder.getTenantId());
            };

            Runnable decoratedTask = decorator.decorate(originalTask);

            // 在子线程中先设置不同的租户
            Thread thread = new Thread(() -> {
                holder.setTenantId("child-tenant");
                decoratedTask.run();
                capturedAfter.append(holder.getTenantId());
            });
            thread.start();
            thread.join(1000);

            // 子线程执行后应该恢复到原来的值
            assertThat(capturedBefore.toString()).isEqualTo("tenant-001");
            assertThat(capturedAfter.toString()).isEqualTo("child-tenant");
        }
    }

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        @Test
        @DisplayName("should handle null snapshot when no tenant set")
        void shouldHandleNullSnapshot_whenNoTenantSet() throws InterruptedException {
            Runnable originalTask = () -> {
                // 不应该抛异常
                assertThat(holder.getTenantId()).isNull();
            };

            Runnable decoratedTask = decorator.decorate(originalTask);

            Thread thread = new Thread(decoratedTask);
            thread.start();
            thread.join(1000);
        }

        @Test
        @DisplayName("should restore previous context after decorated execution")
        void shouldRestorePreviousContext_afterDecoratedExecution() throws InterruptedException {
            holder.setTenantId("parent-tenant");

            Runnable originalTask = () -> {
                // 任务执行期间租户上下文应该是父线程的值
                assertThat(holder.getTenantId()).isEqualTo("parent-tenant");
            };

            Runnable decoratedTask = decorator.decorate(originalTask);

            Thread thread = new Thread(() -> {
                holder.setTenantId("child-tenant");
                decoratedTask.run();
                // 执行后应该恢复到子线程原来的值
                assertThat(holder.getTenantId()).isEqualTo("child-tenant");
            });
            thread.start();
            thread.join(1000);

            // 父线程的值不应该被影响
            assertThat(holder.getTenantId()).isEqualTo("parent-tenant");
        }
    }
}
