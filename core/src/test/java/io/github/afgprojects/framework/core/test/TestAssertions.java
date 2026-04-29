package io.github.afgprojects.framework.core.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.model.exception.ErrorCode;
import io.github.afgprojects.framework.core.model.exception.ErrorCategory;
import io.github.afgprojects.framework.core.model.result.PageData;
import io.github.afgprojects.framework.core.model.result.Result;

/**
 * 测试断言
 * 提供自定义断言方法简化测试验证
 *
 * <p>使用示例:
 * <pre>{@code
 * // 断言 Result 成功
 * TestAssertions.assertResultSuccess(result);
 * TestAssertions.assertResultSuccess(result, "操作成功");
 * TestAssertions.assertResultSuccess(result, expectedData);
 *
 * // 断言 Result 失败
 * TestAssertions.assertResultFailed(result, 10001);
 * TestAssertions.assertResultFailed(result, CommonErrorCode.PARAM_ERROR);
 *
 * // 断言 PageData
 * TestAssertions.assertPageData(pageData, 10, 100);
 * TestAssertions.assertPageDataContains(pageData, expectedItem);
 *
 * // 断言 ErrorCode
 * TestAssertions.assertErrorCode(errorCode, 10001, ErrorCategory.BUSINESS);
 * }</pre>
 */
public final class TestAssertions {

    private TestAssertions() {
        // 工具类，禁止实例化
    }

    // ==================== Result 断言 ====================

    /**
     * 断言 Result 成功
     *
     * @param result Result 对象
     */
    public static void assertResultSuccess(@NonNull Result<?> result) {
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.code()).isZero();
    }

    /**
     * 断言 Result 成功（带消息）
     *
     * @param result  Result 对象
     * @param message 期望的消息
     */
    public static void assertResultSuccess(@NonNull Result<?> result, @NonNull String message) {
        assertResultSuccess(result);
        assertThat(result.message()).isEqualTo(message);
    }

    /**
     * 断言 Result 成功并验证数据
     *
     * @param result       Result 对象
     * @param expectedData 期望的数据
     * @param <T>          数据类型
     */
    public static <T> void assertResultSuccess(@NonNull Result<T> result, @NonNull T expectedData) {
        assertResultSuccess(result);
        assertThat(result.data()).isNotNull();
        assertThat(result.data()).isEqualTo(expectedData);
    }

    /**
     * 断言 Result 成功并验证数据类型
     *
     * @param result      Result 对象
     * @param expectedType 期望的数据类型
     * @param <T>         数据类型
     */
    public static <T> void assertResultSuccess(@NonNull Result<T> result, @NonNull Class<?> expectedType) {
        assertResultSuccess(result);
        assertThat(result.data()).isNotNull();
        assertThat(result.data()).isInstanceOf(expectedType);
    }

    /**
     * 断言 Result 失败
     *
     * @param result Result 对象
     */
    public static void assertResultFailed(@NonNull Result<?> result) {
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.code()).isNotZero();
    }

    /**
     * 断言 Result 失败（带错误码）
     *
     * @param result      Result 对象
     * @param expectedCode 期望的错误码
     */
    public static void assertResultFailed(@NonNull Result<?> result, int expectedCode) {
        assertResultFailed(result);
        assertThat(result.code()).isEqualTo(expectedCode);
    }

    /**
     * 断言 Result 失败（带错误码枚举）
     *
     * @param result      Result 对象
     * @param errorCode   期望的错误码枚举
     */
    public static void assertResultFailed(@NonNull Result<?> result, @NonNull ErrorCode errorCode) {
        assertResultFailed(result);
        assertThat(result.code()).isEqualTo(errorCode.getCode());
    }

    /**
     * 断言 Result 失败（带错误码和消息）
     *
     * @param result         Result 对象
     * @param expectedCode   期望的错误码
     * @param expectedMessage 期望的消息
     */
    public static void assertResultFailed(@NonNull Result<?> result, int expectedCode, @NonNull String expectedMessage) {
        assertResultFailed(result, expectedCode);
        assertThat(result.message()).isEqualTo(expectedMessage);
    }

    /**
     * 断言 Result 包含 traceId
     *
     * @param result Result 对象
     */
    public static void assertResultHasTraceId(@NonNull Result<?> result) {
        assertThat(result.traceId()).isNotNull();
        assertThat(result.traceId()).isNotEmpty();
    }

    /**
     * 断言 Result 包含 requestId
     *
     * @param result Result 对象
     */
    public static void assertResultHasRequestId(@NonNull Result<?> result) {
        assertThat(result.requestId()).isNotNull();
        assertThat(result.requestId()).isNotEmpty();
    }

    // ==================== PageData 断言 ====================

    /**
     * 断言 PageData 不为空
     *
     * @param pageData PageData 对象
     */
    public static void assertPageDataNotEmpty(@NonNull PageData<?> pageData) {
        assertThat(pageData.records()).isNotEmpty();
        assertThat(pageData.total()).isPositive();
    }

    /**
     * 断言 PageData 为空
     *
     * @param pageData PageData 对象
     */
    public static void assertPageDataEmpty(@NonNull PageData<?> pageData) {
        assertThat(pageData.records()).isEmpty();
        assertThat(pageData.total()).isZero();
    }

    /**
     * 断言 PageData 的记录数量
     *
     * @param pageData        PageData 对象
     * @param expectedSize    期望的记录数量
     */
    public static void assertPageDataSize(@NonNull PageData<?> pageData, int expectedSize) {
        assertThat(pageData.records()).hasSize(expectedSize);
    }

    /**
     * 断言 PageData 的总数
     *
     * @param pageData     PageData 对象
     * @param expectedTotal 期望的总数
     */
    public static void assertPageDataTotal(@NonNull PageData<?> pageData, long expectedTotal) {
        assertThat(pageData.total()).isEqualTo(expectedTotal);
    }

    /**
     * 断言 PageData 的页码信息
     *
     * @param pageData     PageData 对象
     * @param expectedPage 期望的页码
     * @param expectedSize 期望的每页大小
     */
    public static void assertPageDataPageInfo(@NonNull PageData<?> pageData, long expectedPage, long expectedSize) {
        assertThat(pageData.page()).isEqualTo(expectedPage);
        assertThat(pageData.size()).isEqualTo(expectedSize);
    }

    /**
     * 断言 PageData 完整信息
     *
     * @param pageData     PageData 对象
     * @param expectedSize 期望的记录数量
     * @param expectedTotal 期望的总数
     */
    public static void assertPageData(@NonNull PageData<?> pageData, int expectedSize, long expectedTotal) {
        assertPageDataSize(pageData, expectedSize);
        assertPageDataTotal(pageData, expectedTotal);
    }

    /**
     * 断言 PageData 包含指定元素
     *
     * @param pageData PageData 对象
     * @param element  期望包含的元素
     * @param <T>      元素类型
     */
    public static <T> void assertPageDataContains(@NonNull PageData<T> pageData, @NonNull T element) {
        assertThat(pageData.records()).contains(element);
    }

    /**
     * 断言 PageData 不包含指定元素
     *
     * @param pageData PageData 对象
     * @param element  期望不包含的元素
     * @param <T>      元素类型
     */
    public static <T> void assertPageDataNotContains(@NonNull PageData<T> pageData, @NonNull T element) {
        assertThat(pageData.records()).doesNotContain(element);
    }

    /**
     * 断言 PageData 有下一页
     *
     * @param pageData PageData 对象
     */
    public static void assertPageDataHasNext(@NonNull PageData<?> pageData) {
        assertThat(pageData.hasNext()).isTrue();
    }

    /**
     * 断言 PageData 没有下一页
     *
     * @param pageData PageData 对象
     */
    public static void assertPageDataNoNext(@NonNull PageData<?> pageData) {
        assertThat(pageData.hasNext()).isFalse();
    }

    // ==================== ErrorCode 断言 ====================

    /**
     * 断言 ErrorCode 的编码
     *
     * @param errorCode   ErrorCode 对象
     * @param expectedCode 期望的编码
     */
    public static void assertErrorCode(@NonNull ErrorCode errorCode, int expectedCode) {
        assertThat(errorCode.getCode()).isEqualTo(expectedCode);
    }

    /**
     * 断言 ErrorCode 的编码和分类
     *
     * @param errorCode        ErrorCode 对象
     * @param expectedCode     期望的编码
     * @param expectedCategory 期望的分类
     */
    public static void assertErrorCode(
            @NonNull ErrorCode errorCode, int expectedCode, @NonNull ErrorCategory expectedCategory) {
        assertThat(errorCode.getCode()).isEqualTo(expectedCode);
        assertThat(errorCode.getCategory()).isEqualTo(expectedCategory);
    }

    /**
     * 断言 ErrorCode 的分类
     *
     * @param errorCode        ErrorCode 对象
     * @param expectedCategory 期望的分类
     */
    public static void assertErrorCodeCategory(@NonNull ErrorCode errorCode, @NonNull ErrorCategory expectedCategory) {
        assertThat(errorCode.getCategory()).isEqualTo(expectedCategory);
    }

    /**
     * 断言 ErrorCode 是业务错误
     *
     * @param errorCode ErrorCode 对象
     */
    public static void assertErrorCodeBusiness(@NonNull ErrorCode errorCode) {
        assertThat(errorCode.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
    }

    /**
     * 断言 ErrorCode 是系统错误
     *
     * @param errorCode ErrorCode 对象
     */
    public static void assertErrorCodeSystem(@NonNull ErrorCode errorCode) {
        assertThat(errorCode.getCategory()).isEqualTo(ErrorCategory.SYSTEM);
    }

    /**
     * 断言 ErrorCode 是安全错误
     *
     * @param errorCode ErrorCode 对象
     */
    public static void assertErrorCodeSecurity(@NonNull ErrorCode errorCode) {
        assertThat(errorCode.getCategory()).isEqualTo(ErrorCategory.SECURITY);
    }

    // ==================== List 断言 ====================

    /**
     * 断言列表不为空
     *
     * @param list 列表
     */
    public static void assertListNotEmpty(@NonNull List<?> list) {
        assertThat(list).isNotEmpty();
    }

    /**
     * 断言列表为空
     *
     * @param list 列表
     */
    public static void assertListEmpty(@NonNull List<?> list) {
        assertThat(list).isEmpty();
    }

    /**
     * 断言列表大小
     *
     * @param list        列表
     * @param expectedSize 期望大小
     */
    public static void assertListSize(@NonNull List<?> list, int expectedSize) {
        assertThat(list).hasSize(expectedSize);
    }

    /**
     * 断言列表包含元素
     *
     * @param list    列表
     * @param element 元素
     * @param <T>     元素类型
     */
    public static <T> void assertListContains(@NonNull List<T> list, @NonNull T element) {
        assertThat(list).contains(element);
    }

    /**
     * 断言列表按指定顺序包含元素
     *
     * @param list     列表
     * @param elements 元素数组
     * @param <T>      元素类型
     */
    @SafeVarargs
    public static <T> void assertListContainsExactly(@NonNull List<T> list, @NonNull T... elements) {
        assertThat(list).containsExactly(elements);
    }

    /**
     * 断言列表包含任意顺序的元素
     *
     * @param list     列表
     * @param elements 元素数组
     * @param <T>      元素类型
     */
    @SafeVarargs
    public static <T> void assertListContainsExactlyInAnyOrder(@NonNull List<T> list, @NonNull T... elements) {
        assertThat(list).containsExactlyInAnyOrder(elements);
    }

    // ==================== 字符串断言 ====================

    /**
     * 断言字符串不为空
     *
     * @param str 字符串
     */
    public static void assertStringNotEmpty(@Nullable String str) {
        assertThat(str).isNotEmpty();
    }

    /**
     * 断言字符串为空或 null
     *
     * @param str 字符串
     */
    public static void assertStringEmpty(@Nullable String str) {
        assertThat(str).isNullOrEmpty();
    }

    /**
     * 断言字符串以指定前缀开头
     *
     * @param str    字符串
     * @param prefix 前缀
     */
    public static void assertStringStartsWith(@NonNull String str, @NonNull String prefix) {
        assertThat(str).startsWith(prefix);
    }

    /**
     * 断言字符串以指定后缀结尾
     *
     * @param str    字符串
     * @param suffix 后缀
     */
    public static void assertStringEndsWith(@NonNull String str, @NonNull String suffix) {
        assertThat(str).endsWith(suffix);
    }

    /**
     * 断言字符串包含子串
     *
     * @param str     字符串
     * @param substring 子串
     */
    public static void assertStringContains(@NonNull String str, @NonNull String substring) {
        assertThat(str).contains(substring);
    }

    /**
     * 断言字符串匹配正则表达式
     *
     * @param str   字符串
     * @param regex 正则表达式
     */
    public static void assertStringMatches(@NonNull String str, @NonNull String regex) {
        assertThat(str).matches(regex);
    }

    // ==================== 数值断言 ====================

    /**
     * 断言数值在范围内
     *
     * @param value 数值
     * @param min   最小值（包含）
     * @param max   最大值（包含）
     */
    public static void assertInRange(int value, int min, int max) {
        assertThat(value).isBetween(min, max);
    }

    /**
     * 断言数值大于指定值
     *
     * @param value  数值
     * @param target 目标值
     */
    public static void assertGreaterThan(int value, int target) {
        assertThat(value).isGreaterThan(target);
    }

    /**
     * 断言数值小于指定值
     *
     * @param value  数值
     * @param target 目标值
     */
    public static void assertLessThan(int value, int target) {
        assertThat(value).isLessThan(target);
    }

    /**
     * 断言数值为正数
     *
     * @param value 数值
     */
    public static void assertPositive(int value) {
        assertThat(value).isPositive();
    }

    /**
     * 断言数值为负数
     *
     * @param value 数值
     */
    public static void assertNegative(int value) {
        assertThat(value).isNegative();
    }

    /**
     * 断言数值为零
     *
     * @param value 数值
     */
    public static void assertZero(int value) {
        assertThat(value).isZero();
    }

    // ==================== 对象断言 ====================

    /**
     * 断言对象不为 null
     *
     * @param object 对象
     */
    public static void assertNotNull(@Nullable Object object) {
        assertThat(object).isNotNull();
    }

    /**
     * 断言对象为 null
     *
     * @param object 对象
     */
    public static void assertNull(@Nullable Object object) {
        assertThat(object).isNull();
    }

    /**
     * 断言两个对象相等
     *
     * @param actual   实际值
     * @param expected 期望值
     */
    public static void assertEquals(@Nullable Object actual, @Nullable Object expected) {
        assertThat(actual).isEqualTo(expected);
    }

    /**
     * 断言两个对象不相等
     *
     * @param actual   实际值
     * @param expected 期望值
     */
    public static void assertNotEquals(@Nullable Object actual, @Nullable Object expected) {
        assertThat(actual).isNotEqualTo(expected);
    }

    /**
     * 断言对象是指定类型
     *
     * @param object 对象
     * @param type   期望类型
     */
    public static void assertInstanceOf(@Nullable Object object, @NonNull Class<?> type) {
        assertThat(object).isInstanceOf(type);
    }
}
