package io.github.afgprojects.framework.ai.core.etl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PromptTemplateTest {

    @Test
    void testSummarizeTemplate() {
        PromptTemplate template = PromptTemplate.summarize();

        assertEquals(PromptType.SUMMARIZE, template.getType());
        assertTrue(template.getTemplate().contains("{content}"));
    }

    @Test
    void testTranslateTemplate() {
        PromptTemplate template = PromptTemplate.translate("英文");

        assertEquals(PromptType.TRANSLATE, template.getType());
        String rendered = template.render(Map.of("content", "你好世界"));
        assertTrue(rendered.contains("英文"));
        assertTrue(rendered.contains("你好世界"));
    }

    @Test
    void testExtractKeywordsTemplate() {
        PromptTemplate template = PromptTemplate.extractKeywords(5);

        assertEquals(PromptType.EXTRACT_KEYWORDS, template.getType());
        String rendered = template.render(Map.of("content", "测试内容"));
        assertTrue(rendered.contains("5"));
    }

    @Test
    void testCustomTemplate() {
        PromptTemplate template = PromptTemplate.custom("自定义提示：{content}");

        assertEquals(PromptType.CUSTOM, template.getType());
        String rendered = template.render(Map.of("content", "内容"));
        assertEquals("自定义提示：内容", rendered);
    }

    @Test
    void testRenderWithMultipleVariables() {
        PromptTemplate template = PromptTemplate.custom("语言：{lang}，内容：{content}");
        String rendered = template.render(Map.of("lang", "中文", "content", "测试"));

        assertEquals("语言：中文，内容：测试", rendered);
    }
}
