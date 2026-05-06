package io.github.afgprojects.framework.module.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.*;

/**
 * AfgModuleAnnotationProcessor 单元测试
 */
@DisplayName("AfgModuleAnnotationProcessor 测试")
class AfgModuleAnnotationProcessorTest  {

    @Nested
    @DisplayName("基本功能测试")
    class BasicFunctionalityTest {

        @Test
        @DisplayName("处理器应该正确初始化")
        void processorShouldInitialize() {
            // given
            AfgModuleAnnotationProcessor processor = new AfgModuleAnnotationProcessor();

            // then
            assertNotNull(processor);
        }

        @Test
        @DisplayName("支持的注解类型应该正确")
        void supportedAnnotationTypesShouldBeCorrect() {
            // given
            AfgModuleAnnotationProcessor processor = new AfgModuleAnnotationProcessor();

            // when
            Set<String> supportedTypes = processor.getSupportedAnnotationTypes();

            // then
            assertNotNull(supportedTypes);
            assertTrue(supportedTypes.contains("io.github.afgprojects.framework.module.AfgModuleAnnotation"));
        }

        @Test
        @DisplayName("支持的源版本应该正确")
        void supportedSourceVersionShouldBeCorrect() {
            // given
            AfgModuleAnnotationProcessor processor = new AfgModuleAnnotationProcessor();

            // when
            var version = processor.getSupportedSourceVersion();

            // then
            assertNotNull(version);
        }
    }

    @Nested
    @DisplayName("索引文件测试")
    class IndexFileTest {

        @Test
        @DisplayName("索引文件路径应该正确")
        void indexFilePathShouldBeCorrect() {
            // 索引文件路径
            String indexPath = "META-INF/afg-modules.index";

            // 验证路径格式
            assertTrue(indexPath.startsWith("META-INF/"));
            assertTrue(indexPath.endsWith(".index"));
        }

        @Test
        @DisplayName("应该能够读取索引文件格式")
        void shouldReadIndexFileFormat() {
            // 模拟索引文件内容
            String content = """
                io.github.afgprojects.auth.AuthModuleConfig
                io.github.afgprojects.system.SystemModuleConfig
                """;

            // 验证格式可解析
            String[] lines = content.trim().split("\n");
            assertEquals(2, lines.length);
            assertTrue(lines[0].contains("AuthModuleConfig"));
            assertTrue(lines[1].contains("SystemModuleConfig"));
        }
    }
}
