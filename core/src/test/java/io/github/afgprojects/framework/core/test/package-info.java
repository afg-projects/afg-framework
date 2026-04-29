/**
 * 测试工具包
 * 提供增强的测试工具提高测试效率
 *
 * <p>包含以下工具类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.test.MockMvcExtensions} - MockMvc 扩展方法，简化 API 测试</li>
 *   <li>{@link io.github.afgprojects.framework.core.test.TestDataBuilder} - 测试数据构建器，提供流式 API 构建测试数据</li>
 *   <li>{@link io.github.afgprojects.framework.core.test.TestFixtures} - 常用测试数据固件</li>
 *   <li>{@link io.github.afgprojects.framework.core.test.MockUtils} - Mock 工具类，简化 Mock 对象创建和配置</li>
 *   <li>{@link io.github.afgprojects.framework.core.test.TestAssertions} - 自定义断言方法</li>
 *   <li>{@link io.github.afgprojects.framework.core.test.ContractTestGenerator} - 契约测试自动生成</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 构建测试数据
 * TestDataBuilder.UserData user = TestDataBuilder.user()
 *     .id("user-001")
 *     .username("testuser")
 *     .build();
 *
 * // 使用固件数据
 * TestDataBuilder.UserData defaultUser = TestFixtures.DEFAULT_USER;
 *
 * // 使用断言
 * TestAssertions.assertResultSuccess(result);
 * TestAssertions.assertPageData(pageData, 10, 100);
 *
 * // 使用 MockUtils
 * UserService mock = MockUtils.mock(UserService.class, m -> {
 *     when(m.findById("001")).thenReturn(user);
 * });
 *
 * // 使用 MockMvcExtensions
 * MockMvcExtensions.perform(mockMvc, objectMapper)
 *     .get("/api/users/1")
 *     .expectOk()
 *     .expectBody(User.class);
 *
 * // 生成契约
 * ContractTestGenerator.generateContract(UserController.class, "contracts/user-api.json");
 * }</pre>
 */
package io.github.afgprojects.framework.core.test;
