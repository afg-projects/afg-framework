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
package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuditableContext 接口契约测试
 */
@DisplayName("AuditableContext 接口契约测试")
class AuditableContextTest {

    @Nested
    @DisplayName("自定义实现")
    class CustomImplementationTests {

        @Test
        @DisplayName("自定义实现应能返回用户 ID")
        void shouldReturnUserId_fromCustomImplementation() {
            AuditableContext customContext = () -> "user-123";
            assertThat(customContext.getCurrentUserId()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("自定义实现可返回 null（未登录场景）")
        void shouldReturnNull_whenNotAuthenticated() {
            AuditableContext unauthenticatedContext = () -> null;
            assertThat(unauthenticatedContext.getCurrentUserId()).isNull();
        }

        @Test
        @DisplayName("自定义实现可返回空字符串")
        void shouldReturnEmptyString() {
            AuditableContext emptyContext = () -> "";
            assertThat(emptyContext.getCurrentUserId()).isEmpty();
        }
    }

    @Nested
    @DisplayName("NoOp 降级行为")
    class NoOpFallbackTests {

        @Test
        @DisplayName("NoOpAuditableContext 应返回 system，避免 DB NOT NULL 约束违反")
        void shouldReturnNull_fromNoOpFallback() {
            AuditableContext noOp = new NoOpAuditableContext();
            assertThat(noOp.getCurrentUserId()).isEqualTo("system");
        }
    }
}
