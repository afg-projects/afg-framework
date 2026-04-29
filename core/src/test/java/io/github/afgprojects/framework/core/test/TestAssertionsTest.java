package io.github.afgprojects.framework.core.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;
import io.github.afgprojects.framework.core.model.exception.ErrorCategory;
import io.github.afgprojects.framework.core.model.result.PageData;
import io.github.afgprojects.framework.core.model.result.Result;
import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * TestAssertions 测试
 */
class TestAssertionsTest extends BaseUnitTest {

    @Test
    @DisplayName("断言 Result 成功")
    void shouldAssertResultSuccess() {
        Result<String> result = Result.success("data");

        assertThatNoException().isThrownBy(() -> TestAssertions.assertResultSuccess(result));
    }

    @Test
    @DisplayName("断言 Result 成功带消息")
    void shouldAssertResultSuccessWithMessage() {
        Result<String> result = Result.success("操作成功", "data");

        // 转换为 Result<?> 以显式调用 assertResultSuccess(Result<?>, String) 进行消息检查
        assertThatNoException().isThrownBy(() -> TestAssertions.assertResultSuccess((Result<?>) result, "操作成功"));
    }

    @Test
    @DisplayName("断言 Result 成功带数据")
    void shouldAssertResultSuccessWithData() {
        Result<String> result = Result.success("data");

        // 直接使用 assertThat 以避免 assertResultSuccess(Result<?>, String) 消息重载和 assertResultSuccess<T>(Result<T>, T) 数据重载之间的歧义
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data()).isEqualTo("data");
    }

    @Test
    @DisplayName("断言 Result 成功带类型")
    void shouldAssertResultSuccessWithType() {
        Result<String> result = Result.success("data");

        assertThatNoException().isThrownBy(() -> TestAssertions.assertResultSuccess(result, String.class));
    }

    @Test
    @DisplayName("断言 Result 失败")
    void shouldAssertResultFailed() {
        Result<Void> result = Result.fail(10001, "错误");

        assertThatNoException().isThrownBy(() -> TestAssertions.assertResultFailed(result));
    }

    @Test
    @DisplayName("断言 Result 失败带错误码")
    void shouldAssertResultFailedWithCode() {
        Result<Void> result = Result.fail(10001, "错误");

        assertThatNoException().isThrownBy(() -> TestAssertions.assertResultFailed(result, 10001));
    }

    @Test
    @DisplayName("断言 Result 失败带错误码枚举")
    void shouldAssertResultFailedWithErrorCode() {
        Result<Void> result = Result.fail(CommonErrorCode.FAIL.getCode(), CommonErrorCode.FAIL.getMessage());

        assertThatNoException().isThrownBy(() -> TestAssertions.assertResultFailed(result, CommonErrorCode.FAIL));
    }

    @Test
    @DisplayName("断言 Result 失败带错误码和消息")
    void shouldAssertResultFailedWithCodeAndMessage() {
        Result<Void> result = Result.fail(10002, "参数错误");

        assertThatNoException().isThrownBy(() -> TestAssertions.assertResultFailed(result, 10002, "参数错误"));
    }

    @Test
    @DisplayName("断言 PageData 不为空")
    void shouldAssertPageDataNotEmpty() {
        PageData<String> pageData = PageData.of(List.of("a", "b"), 2, 1, 10);

        assertThatNoException().isThrownBy(() -> TestAssertions.assertPageDataNotEmpty(pageData));
    }

    @Test
    @DisplayName("断言 PageData 为空")
    void shouldAssertPageDataEmpty() {
        PageData<String> pageData = PageData.empty();

        assertThatNoException().isThrownBy(() -> TestAssertions.assertPageDataEmpty(pageData));
    }

    @Test
    @DisplayName("断言 PageData 记录数量")
    void shouldAssertPageDataSize() {
        PageData<String> pageData = PageData.of(List.of("a", "b", "c"), 3, 1, 10);

        assertThatNoException().isThrownBy(() -> TestAssertions.assertPageDataSize(pageData, 3));
    }

    @Test
    @DisplayName("断言 PageData 总数")
    void shouldAssertPageDataTotal() {
        PageData<String> pageData = PageData.of(List.of("a", "b"), 100, 1, 10);

        assertThatNoException().isThrownBy(() -> TestAssertions.assertPageDataTotal(pageData, 100));
    }

    @Test
    @DisplayName("断言 PageData 页码信息")
    void shouldAssertPageDataPageInfo() {
        PageData<String> pageData = PageData.of(List.of("a"), 100, 2, 10);

        assertThatNoException().isThrownBy(() -> TestAssertions.assertPageDataPageInfo(pageData, 2, 10));
    }

    @Test
    @DisplayName("断言 PageData 完整信息")
    void shouldAssertPageData() {
        PageData<String> pageData = PageData.of(List.of("a", "b", "c"), 100, 1, 10);

        assertThatNoException().isThrownBy(() -> TestAssertions.assertPageData(pageData, 3, 100));
    }

    @Test
    @DisplayName("断言 PageData 包含元素")
    void shouldAssertPageDataContains() {
        PageData<String> pageData = PageData.of(List.of("a", "b", "c"), 3, 1, 10);

        assertThatNoException().isThrownBy(() -> TestAssertions.assertPageDataContains(pageData, "b"));
    }

    @Test
    @DisplayName("断言 PageData 不包含元素")
    void shouldAssertPageDataNotContains() {
        PageData<String> pageData = PageData.of(List.of("a", "b", "c"), 3, 1, 10);

        assertThatNoException().isThrownBy(() -> TestAssertions.assertPageDataNotContains(pageData, "d"));
    }

    @Test
    @DisplayName("断言 PageData 有下一页")
    void shouldAssertPageDataHasNext() {
        PageData<String> pageData = PageData.of(List.of("a"), 100, 1, 10);

        assertThatNoException().isThrownBy(() -> TestAssertions.assertPageDataHasNext(pageData));
    }

    @Test
    @DisplayName("断言 PageData 没有下一页")
    void shouldAssertPageDataNoNext() {
        PageData<String> pageData = PageData.of(List.of("a"), 1, 1, 10);

        assertThatNoException().isThrownBy(() -> TestAssertions.assertPageDataNoNext(pageData));
    }

    @Test
    @DisplayName("断言 ErrorCode 编码")
    void shouldAssertErrorCode() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertErrorCode(CommonErrorCode.FAIL, 10001));
    }

    @Test
    @DisplayName("断言 ErrorCode 编码和分类")
    void shouldAssertErrorCodeWithCategory() {
        assertThatNoException()
                .isThrownBy(() -> TestAssertions.assertErrorCode(CommonErrorCode.FAIL, 10001, ErrorCategory.BUSINESS));
    }

    @Test
    @DisplayName("断言 ErrorCode 分类")
    void shouldAssertErrorCodeCategory() {
        assertThatNoException()
                .isThrownBy(() -> TestAssertions.assertErrorCodeCategory(CommonErrorCode.FAIL, ErrorCategory.BUSINESS));
    }

    @Test
    @DisplayName("断言 ErrorCode 是业务错误")
    void shouldAssertErrorCodeBusiness() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertErrorCodeBusiness(CommonErrorCode.FAIL));
    }

    @Test
    @DisplayName("断言列表不为空")
    void shouldAssertListNotEmpty() {
        List<String> list = List.of("a", "b", "c");

        assertThatNoException().isThrownBy(() -> TestAssertions.assertListNotEmpty(list));
    }

    @Test
    @DisplayName("断言列表为空")
    void shouldAssertListEmpty() {
        List<String> list = List.of();

        assertThatNoException().isThrownBy(() -> TestAssertions.assertListEmpty(list));
    }

    @Test
    @DisplayName("断言列表大小")
    void shouldAssertListSize() {
        List<String> list = List.of("a", "b", "c");

        assertThatNoException().isThrownBy(() -> TestAssertions.assertListSize(list, 3));
    }

    @Test
    @DisplayName("断言列表包含元素")
    void shouldAssertListContains() {
        List<String> list = List.of("a", "b", "c");

        assertThatNoException().isThrownBy(() -> TestAssertions.assertListContains(list, "b"));
    }

    @Test
    @DisplayName("断言列表按顺序包含元素")
    void shouldAssertListContainsExactly() {
        List<String> list = List.of("a", "b", "c");

        assertThatNoException().isThrownBy(() -> TestAssertions.assertListContainsExactly(list, "a", "b", "c"));
    }

    @Test
    @DisplayName("断言列表包含任意顺序元素")
    void shouldAssertListContainsExactlyInAnyOrder() {
        List<String> list = List.of("c", "a", "b");

        assertThatNoException().isThrownBy(() -> TestAssertions.assertListContainsExactlyInAnyOrder(list, "a", "b", "c"));
    }

    @Test
    @DisplayName("断言字符串不为空")
    void shouldAssertStringNotEmpty() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertStringNotEmpty("test"));
    }

    @Test
    @DisplayName("断言字符串为空")
    void shouldAssertStringEmpty() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertStringEmpty(""));
        assertThatNoException().isThrownBy(() -> TestAssertions.assertStringEmpty(null));
    }

    @Test
    @DisplayName("断言字符串以指定前缀开头")
    void shouldAssertStringStartsWith() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertStringStartsWith("hello world", "hello"));
    }

    @Test
    @DisplayName("断言字符串以指定后缀结尾")
    void shouldAssertStringEndsWith() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertStringEndsWith("hello world", "world"));
    }

    @Test
    @DisplayName("断言字符串包含子串")
    void shouldAssertStringContains() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertStringContains("hello world", "lo wo"));
    }

    @Test
    @DisplayName("断言字符串匹配正则")
    void shouldAssertStringMatches() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertStringMatches("abc123", "[a-z]+[0-9]+"));
    }

    @Test
    @DisplayName("断言数值在范围内")
    void shouldAssertInRange() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertInRange(5, 1, 10));
    }

    @Test
    @DisplayName("断言数值大于指定值")
    void shouldAssertGreaterThan() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertGreaterThan(10, 5));
    }

    @Test
    @DisplayName("断言数值小于指定值")
    void shouldAssertLessThan() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertLessThan(5, 10));
    }

    @Test
    @DisplayName("断言数值为正数")
    void shouldAssertPositive() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertPositive(10));
    }

    @Test
    @DisplayName("断言数值为负数")
    void shouldAssertNegative() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertNegative(-10));
    }

    @Test
    @DisplayName("断言数值为零")
    void shouldAssertZero() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertZero(0));
    }

    @Test
    @DisplayName("断言对象不为 null")
    void shouldAssertNotNull() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertNotNull("test"));
    }

    @Test
    @DisplayName("断言对象为 null")
    void shouldAssertNull() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertNull(null));
    }

    @Test
    @DisplayName("断言两个对象相等")
    void shouldAssertEquals() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertEquals("test", "test"));
    }

    @Test
    @DisplayName("断言两个对象不相等")
    void shouldAssertNotEquals() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertNotEquals("test", "other"));
    }

    @Test
    @DisplayName("断言对象是指定类型")
    void shouldAssertInstanceOf() {
        assertThatNoException().isThrownBy(() -> TestAssertions.assertInstanceOf("test", String.class));
    }
}