package io.github.afgprojects.framework.core.security.datascope.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.security.datascope.DataScopeProperties;
import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * DataScope 集成测试
 */
@DisplayName("DataScope 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.data-scope.enabled=true",
                "afg.data-scope.default-scope-type=DEPT"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataScopeIntegrationTest {

    @Autowired(required = false)
    private DataScopeProperties dataScopeProperties;

    @Nested
    @DisplayName("DataScope 配置测试")
    class DataScopeConfigTests {

        @Test
        @DisplayName("应该正确配置 DataScopeProperties")
        void shouldConfigureDataScopeProperties() {
            assertThat(dataScopeProperties).isNotNull();
            assertThat(dataScopeProperties.isEnabled()).isTrue();
        }
    }
}