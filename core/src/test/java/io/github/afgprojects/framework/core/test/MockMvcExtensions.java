package io.github.afgprojects.framework.core.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MockMvc 扩展方法
 * 提供流式 API 简化 API 测试
 *
 * <p>使用示例:
 * <pre>{@code
 * // GET 请求
 * MockMvcExtensions.perform(mockMvc, objectMapper)
 *     .get("/api/users/1")
 *     .expectStatus(200)
 *     .expectBody(User.class)
 *     .andReturn();
 *
 * // POST 请求
 * MockMvcExtensions.perform(mockMvc, objectMapper)
 *     .post("/api/users", userDto)
 *     .expectStatus(201)
 *     .expectHeader("Location", "/api/users/1")
 *     .andReturn();
 * }</pre>
 */
public class MockMvcExtensions {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final Map<String, String> defaultHeaders = new HashMap<>();

    /**
     * 创建 MockMvcExtensions 实例
     *
     * @param mockMvc      MockMvc 实例
     * @param objectMapper ObjectMapper 实例
     */
    public MockMvcExtensions(@NonNull MockMvc mockMvc, @NonNull ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建 MockMvcExtensions 实例（静态工厂方法）
     *
     * @param mockMvc      MockMvc 实例
     * @param objectMapper ObjectMapper 实例
     * @return MockMvcExtensions 实例
     */
    public static MockMvcExtensions perform(@NonNull MockMvc mockMvc, @NonNull ObjectMapper objectMapper) {
        return new MockMvcExtensions(mockMvc, objectMapper);
    }

    /**
     * 添加默认请求头
     *
     * @param name  请求头名称
     * @param value 请求头值
     * @return this
     */
    public MockMvcExtensions withDefaultHeader(@NonNull String name, @NonNull String value) {
        defaultHeaders.put(name, value);
        return this;
    }

    /**
     * 设置 Authorization 请求头
     *
     * @param token 认证令牌
     * @return this
     */
    public MockMvcExtensions withAuthToken(@NonNull String token) {
        defaultHeaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return this;
    }

    /**
     * 执行 GET 请求
     *
     * @param url     请求 URL
     * @param uriVars URL 变量
     * @return RequestBuilder 包装器
     */
    public RequestBuilder get(@NonNull String url, Object... uriVars) {
        return new RequestBuilder(mockMvc, objectMapper, defaultHeaders)
                .method(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url, uriVars));
    }

    /**
     * 执行 GET 请求（带请求头）
     *
     * @param url     请求 URL
     * @param headers 请求头
     * @param uriVars URL 变量
     * @return RequestBuilder 包装器
     */
    public RequestBuilder get(@NonNull String url, @NonNull Map<String, String> headers, Object... uriVars) {
        return new RequestBuilder(mockMvc, objectMapper, defaultHeaders)
                .method(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url, uriVars))
                .headers(headers);
    }

    /**
     * 执行 POST 请求
     *
     * @param url  请求 URL
     * @param body 请求体
     * @return RequestBuilder 包装器
     */
    public RequestBuilder post(@NonNull String url, @NonNull Object body) {
        return new RequestBuilder(mockMvc, objectMapper, defaultHeaders)
                .method(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url))
                .body(body);
    }

    /**
     * 执行 POST 请求（带请求头）
     *
     * @param url     请求 URL
     * @param body    请求体
     * @param headers 请求头
     * @return RequestBuilder 包装器
     */
    public RequestBuilder post(@NonNull String url, @NonNull Object body, @NonNull Map<String, String> headers) {
        return new RequestBuilder(mockMvc, objectMapper, defaultHeaders)
                .method(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url))
                .body(body)
                .headers(headers);
    }

    /**
     * 执行 PUT 请求
     *
     * @param url  请求 URL
     * @param body 请求体
     * @return RequestBuilder 包装器
     */
    public RequestBuilder put(@NonNull String url, @NonNull Object body) {
        return new RequestBuilder(mockMvc, objectMapper, defaultHeaders)
                .method(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url))
                .body(body);
    }

    /**
     * 执行 PUT 请求（带请求头）
     *
     * @param url     请求 URL
     * @param body    请求体
     * @param headers 请求头
     * @return RequestBuilder 包装器
     */
    public RequestBuilder put(@NonNull String url, @NonNull Object body, @NonNull Map<String, String> headers) {
        return new RequestBuilder(mockMvc, objectMapper, defaultHeaders)
                .method(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url))
                .body(body)
                .headers(headers);
    }

    /**
     * 执行 PATCH 请求
     *
     * @param url  请求 URL
     * @param body 请求体
     * @return RequestBuilder 包装器
     */
    public RequestBuilder patch(@NonNull String url, @NonNull Object body) {
        return new RequestBuilder(mockMvc, objectMapper, defaultHeaders)
                .method(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url))
                .body(body);
    }

    /**
     * 执行 DELETE 请求
     *
     * @param url     请求 URL
     * @param uriVars URL 变量
     * @return RequestBuilder 包装器
     */
    public RequestBuilder delete(@NonNull String url, Object... uriVars) {
        return new RequestBuilder(mockMvc, objectMapper, defaultHeaders)
                .method(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url, uriVars));
    }

    /**
     * 执行自定义请求
     *
     * @param builder 请求构建器
     * @return RequestBuilder 包装器
     */
    public RequestBuilder request(@NonNull MockHttpServletRequestBuilder builder) {
        return new RequestBuilder(mockMvc, objectMapper, defaultHeaders).method(builder);
    }

    /**
     * 请求构建器包装类
     */
    public static class RequestBuilder {

        private final MockMvc mockMvc;
        private final ObjectMapper objectMapper;
        private final Map<String, String> defaultHeaders;
        private MockHttpServletRequestBuilder requestBuilder;
        private ResultActions resultActions;

        public RequestBuilder(
                @NonNull MockMvc mockMvc,
                @NonNull ObjectMapper objectMapper,
                @NonNull Map<String, String> defaultHeaders) {
            this.mockMvc = mockMvc;
            this.objectMapper = objectMapper;
            this.defaultHeaders = new HashMap<>(defaultHeaders);
        }

        /**
         * 设置请求方法
         */
        RequestBuilder method(@NonNull MockHttpServletRequestBuilder builder) {
            this.requestBuilder = builder;
            // 添加默认请求头
            defaultHeaders.forEach(requestBuilder::header);
            return this;
        }

        /**
         * 设置请求体
         */
        public RequestBuilder body(@NonNull Object body) {
            try {
                requestBuilder.contentType(MediaType.APPLICATION_JSON);
                requestBuilder.content(objectMapper.writeValueAsString(body));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize request body", e);
            }
            return this;
        }

        /**
         * 设置请求体（JSON 字符串）
         */
        public RequestBuilder bodyJson(@NonNull String json) {
            requestBuilder.contentType(MediaType.APPLICATION_JSON);
            requestBuilder.content(json);
            return this;
        }

        /**
         * 添加请求头
         */
        public RequestBuilder header(@NonNull String name, @NonNull String value) {
            requestBuilder.header(name, value);
            return this;
        }

        /**
         * 批量添加请求头
         */
        public RequestBuilder headers(@NonNull Map<String, String> headers) {
            headers.forEach(requestBuilder::header);
            return this;
        }

        /**
         * 设置 Authorization 请求头
         */
        public RequestBuilder auth(@NonNull String token) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return this;
        }

        /**
         * 设置查询参数
         */
        public RequestBuilder param(@NonNull String name, @NonNull String value) {
            requestBuilder.param(name, value);
            return this;
        }

        /**
         * 批量设置查询参数
         */
        public RequestBuilder params(@NonNull Map<String, String> params) {
            params.forEach(requestBuilder::param);
            return this;
        }

        /**
         * 执行请求并期望状态码
         */
        public RequestBuilder expectStatus(int status) throws Exception {
            execute();
            resultActions.andExpect(MockMvcResultMatchers.status().is(status));
            return this;
        }

        /**
         * 执行请求并期望状态码为 200 OK
         */
        public RequestBuilder expectOk() throws Exception {
            return expectStatus(200);
        }

        /**
         * 执行请求并期望状态码为 201 Created
         */
        public RequestBuilder expectCreated() throws Exception {
            return expectStatus(201);
        }

        /**
         * 执行请求并期望状态码为 204 No Content
         */
        public RequestBuilder expectNoContent() throws Exception {
            return expectStatus(204);
        }

        /**
         * 执行请求并期望状态码为 400 Bad Request
         */
        public RequestBuilder expectBadRequest() throws Exception {
            return expectStatus(400);
        }

        /**
         * 执行请求并期望状态码为 401 Unauthorized
         */
        public RequestBuilder expectUnauthorized() throws Exception {
            return expectStatus(401);
        }

        /**
         * 执行请求并期望状态码为 403 Forbidden
         */
        public RequestBuilder expectForbidden() throws Exception {
            return expectStatus(403);
        }

        /**
         * 执行请求并期望状态码为 404 Not Found
         */
        public RequestBuilder expectNotFound() throws Exception {
            return expectStatus(404);
        }

        /**
         * 解析响应体为指定类型
         */
        public <T> T expectBody(@NonNull Class<T> type) throws Exception {
            execute();
            String content = resultActions.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(content, type);
        }

        /**
         * 期望响应头
         */
        public RequestBuilder expectHeader(@NonNull String name, @NonNull String value) throws Exception {
            execute();
            resultActions.andExpect(MockMvcResultMatchers.header().string(name, value));
            return this;
        }

        /**
         * 期望响应头存在
         */
        public RequestBuilder expectHeaderExists(@NonNull String name) throws Exception {
            execute();
            resultActions.andExpect(MockMvcResultMatchers.header().exists(name));
            return this;
        }

        /**
         * 期望 JSON 路径值
         */
        public RequestBuilder expectJsonPath(@NonNull String path, @NonNull Object value) throws Exception {
            execute();
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(path).value(value));
            return this;
        }

        /**
         * 期望 JSON 路径存在
         */
        public RequestBuilder expectJsonPathExists(@NonNull String path) throws Exception {
            execute();
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(path).exists());
            return this;
        }

        /**
         * 期望 JSON 路径不存在
         */
        public RequestBuilder expectJsonPathNotExists(@NonNull String path) throws Exception {
            execute();
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(path).doesNotExist());
            return this;
        }

        /**
         * 执行请求
         */
        private void execute() throws Exception {
            if (resultActions == null) {
                resultActions = mockMvc.perform(requestBuilder);
            }
        }

        /**
         * 返回 ResultActions
         */
        public ResultActions andReturn() throws Exception {
            execute();
            return resultActions;
        }

        /**
         * 获取响应内容
         */
        public String getResponseContent() throws Exception {
            execute();
            return resultActions.andReturn().getResponse().getContentAsString();
        }

        /**
         * 获取响应状态码
         */
        public int getResponseStatus() throws Exception {
            execute();
            return resultActions.andReturn().getResponse().getStatus();
        }
    }
}
