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
 * NoOpAuditableContext 测试
 */
@DisplayName("NoOpAuditableContext 测试")
class NoOpAuditableContextTest {

    private final NoOpAuditableContext auditableContext = new NoOpAuditableContext();

    @Nested
    @DisplayName("getCurrentUserId 方法")
    class GetCurrentUserIdTests {

        @Test
        @DisplayName("应返回 \"system\"（NoOp 降级，避免 DB NOT NULL 约束违反）")
        void shouldReturnSystem() {
            assertThat(auditableContext.getCurrentUserId()).isEqualTo("system");
        }
    }

    @Nested
    @DisplayName("AuditableContext 接口契约")
    class InterfaceContractTests {

        @Test
        @DisplayName("NoOpAuditableContext 应实现 AuditableContext 接口")
        void shouldImplementAuditableContextInterface() {
            assertThat(auditableContext).isInstanceOf(AuditableContext.class);
        }
    }
}
