package io.github.afgprojects.framework.core.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.afgprojects.framework.core.model.result.Result;
import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * MockMvcExtensions 测试
 */
@ExtendWith(MockitoExtension.class)
class MockMvcExtensionsTest extends BaseUnitTest {

    @Mock
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;
    private MockMvcExtensions extensions;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        extensions = new MockMvcExtensions(mockMvc, objectMapper);
    }

    @Test
    @DisplayName("创建 MockMvcExtensions 实例")
    void shouldCreateInstance() {
        MockMvcExtensions instance = MockMvcExtensions.perform(mockMvc, objectMapper);
        assertThat(instance).isNotNull();
    }

    @Test
    @DisplayName("添加默认请求头")
    void shouldAddDefaultHeader() {
        MockMvcExtensions result = extensions.withDefaultHeader("X-Custom", "value");
        assertThat(result).isSameAs(extensions);
    }

    @Test
    @DisplayName("设置 Authorization 请求头")
    void shouldSetAuthToken() {
        MockMvcExtensions result = extensions.withAuthToken("test-token");
        assertThat(result).isSameAs(extensions);
    }

    @Test
    @DisplayName("创建 GET 请求构建器")
    void shouldCreateGetRequestBuilder() {
        MockMvcExtensions.RequestBuilder builder = extensions.get("/api/test");
        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("创建 POST 请求构建器")
    void shouldCreatePostRequestBuilder() {
        MockMvcExtensions.RequestBuilder builder = extensions.post("/api/test", Map.of("key", "value"));
        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("创建 PUT 请求构建器")
    void shouldCreatePutRequestBuilder() {
        MockMvcExtensions.RequestBuilder builder = extensions.put("/api/test/1", Map.of("key", "value"));
        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("创建 PATCH 请求构建器")
    void shouldCreatePatchRequestBuilder() {
        MockMvcExtensions.RequestBuilder builder = extensions.patch("/api/test/1", Map.of("key", "value"));
        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("创建 DELETE 请求构建器")
    void shouldCreateDeleteRequestBuilder() {
        MockMvcExtensions.RequestBuilder builder = extensions.delete("/api/test/1");
        assertThat(builder).isNotNull();
    }
}
