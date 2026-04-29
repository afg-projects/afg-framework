package io.github.afgprojects.framework.core.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration;

/**
 * WebMvc 测试基类
 * 提供 MockMvc 相关的便捷方法
 *
 * <p>使用示例:
 * <pre>{@code
 * @WebMvcTest(UserController.class)
 * class UserControllerTest extends BaseWebMvcTest {
 *     @Test
 *     void shouldReturnUser() throws Exception {
 *         performGet("/api/users/1")
 *             .andExpect(status().isOk())
 *             .andExpect(jsonPath("$.data.id").value(1));
 *     }
 * }
 * }</pre>
 */
@ImportAutoConfiguration(AfgAutoConfiguration.class)
public abstract class BaseWebMvcTest {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected ObjectMapper objectMapper;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpWebMvcTest() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * 执行 GET 请求
     */
    protected ResultActions performGet(@NonNull String url, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars)).andDo(print());
    }

    /**
     * 执行 POST 请求（JSON body）
     */
    protected ResultActions performPost(@NonNull String url, @NonNull Object body) throws Exception {
        return mockMvc.perform(post(url).contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    /**
     * 执行 PUT 请求（JSON body）
     */
    protected ResultActions performPut(@NonNull String url, @NonNull Object body) throws Exception {
        return mockMvc.perform(put(url).contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    /**
     * 执行 PATCH 请求（JSON body）
     */
    protected ResultActions performPatch(@NonNull String url, @NonNull Object body) throws Exception {
        return mockMvc.perform(
                        patch(url).contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    /**
     * 执行 DELETE 请求
     */
    protected ResultActions performDelete(@NonNull String url, Object... uriVars) throws Exception {
        return mockMvc.perform(delete(url, uriVars)).andDo(print());
    }

    /**
     * 执行自定义请求
     */
    protected ResultActions perform(@NonNull MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andDo(print());
    }

    /**
     * 解析响应为指定类型
     */
    protected <T> T parseResponse(@NonNull MvcResult result, @NonNull Class<T> type) throws Exception {
        String content = result.getResponse().getContentAsString();
        return objectMapper.readValue(content, type);
    }

    /**
     * 获取响应内容
     */
    protected String getResponseContent(@NonNull MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    /**
     * 获取响应状态码
     */
    protected int getStatus(@NonNull MvcResult result) {
        return result.getResponse().getStatus();
    }
}
