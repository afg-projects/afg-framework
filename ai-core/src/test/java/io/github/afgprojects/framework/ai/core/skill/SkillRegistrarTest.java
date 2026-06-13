package io.github.afgprojects.framework.ai.core.skill;

import io.github.afgprojects.framework.ai.core.api.skill.SkillDefinition;
import io.github.afgprojects.framework.ai.core.api.skill.SkillRegistry;
import io.github.afgprojects.framework.ai.core.skill.annotation.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * SkillRegistrar 纯单元测试。
 *
 * <p>使用真实的 DefaultSkillRegistry 进行测试，不使用 mock。
 */
@DisplayName("SkillRegistrar")
class SkillRegistrarTest {

    private final DefaultSkillRegistry registry = new DefaultSkillRegistry();
    private final SkillRegistrar registrar = new SkillRegistrar(registry);

    // ── 测试用 Bean 类 ──────────────────────────────────────────────────────────

    @org.springframework.stereotype.Service
    static class TestSkillService {

        @Skill(name = "query-user", description = "查询用户信息", intentKeywords = {"用户", "查询"})
        public String queryUser(String username) {
            return "User: " + username;
        }

        @Skill(name = "refund", description = "退款处理", intentKeywords = {"退款", "退钱"}, category = "finance")
        public String handleRefund(String orderId, Double amount) {
            return "Refund: " + orderId;
        }

        // 无 @Skill 注解的方法，应被忽略
        public String normalMethod() {
            return "normal";
        }
    }

    @org.springframework.stereotype.Service
    static class SingleSkillService {

        @Skill(name = "simple-skill")
        public String simple() {
            return "simple";
        }
    }

    // ── 测试 ────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("scanAndRegister")
    class ScanAndRegister {

        @Test
        @DisplayName("应扫描 @Skill 注解方法并注册到 SkillRegistry")
        void shouldRegisterSkillMethods_whenScanningBeanWithSkillAnnotations() {
            TestSkillService bean = new TestSkillService();

            int count = registrar.scanAndRegister(bean);

            assertThat(count).isEqualTo(2);
            assertThat(registry.exists("query-user")).isTrue();
            assertThat(registry.exists("refund")).isTrue();
        }

        @Test
        @DisplayName("应忽略无 @Skill 注解的方法")
        void shouldIgnoreMethodsWithoutSkillAnnotation() {
            SingleSkillService bean = new SingleSkillService();

            int count = registrar.scanAndRegister(bean);

            assertThat(count).isEqualTo(1);
            assertThat(registry.exists("simple-skill")).isTrue();
        }

        @Test
        @DisplayName("应正确注册 Skill 的名称和描述")
        void shouldRegisterCorrectNameAndDescription() {
            TestSkillService bean = new TestSkillService();
            registrar.scanAndRegister(bean);

            SkillDefinition definition = registry.get("query-user").orElseThrow();
            assertThat(definition.name()).isEqualTo("query-user");
            assertThat(definition.description()).isEqualTo("查询用户信息");
        }

        @Test
        @DisplayName("应将 intentKeywords 存入 metadata")
        void shouldStoreIntentKeywordsInMetadata() {
            TestSkillService bean = new TestSkillService();
            registrar.scanAndRegister(bean);

            SkillDefinition definition = registry.get("query-user").orElseThrow();
            assertThat(definition.metadata()).isNotNull();
            @SuppressWarnings("unchecked")
            List<String> keywords = (List<String>) definition.metadata().get("intentKeywords");
            assertThat(keywords).containsExactly("用户", "查询");
        }

        @Test
        @DisplayName("应将 category 存入 metadata")
        void shouldStoreCategoryInMetadata() {
            TestSkillService bean = new TestSkillService();
            registrar.scanAndRegister(bean);

            SkillDefinition definition = registry.get("refund").orElseThrow();
            assertThat(definition.metadata()).isNotNull();
            assertThat(definition.metadata().get("category")).isEqualTo("finance");
        }

        @Test
        @DisplayName("无 @Skill 注解的 Bean 应注册 0 个 Skill")
        void shouldRegisterZeroSkills_whenBeanHasNoSkillAnnotation() {
            Object bean = new Object();

            int count = registrar.scanAndRegister(bean);

            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("createDefinition")
    class CreateDefinition {

        @Test
        @DisplayName("应从方法签名生成默认提示词模板")
        void shouldGenerateDefaultPromptFromMethod() throws Exception {
            Method method = TestSkillService.class.getDeclaredMethod("queryUser", String.class);
            Skill annotation = method.getAnnotation(Skill.class);

            SkillDefinition definition = registrar.createDefinition(annotation, method);

            assertThat(definition.prompt()).contains("queryUser");
            assertThat(definition.prompt()).contains("{{username}}");
        }

        @Test
        @DisplayName("应从方法参数构建输入参数定义")
        void shouldBuildInputParametersFromMethod() throws Exception {
            Method method = TestSkillService.class.getDeclaredMethod("handleRefund", String.class, Double.class);
            Skill annotation = method.getAnnotation(Skill.class);

            SkillDefinition definition = registrar.createDefinition(annotation, method);

            assertThat(definition.inputs()).isNotNull();
            assertThat(definition.inputs()).hasSize(2);
            assertThat(definition.inputs().get(0).name()).isEqualTo("orderId");
            assertThat(definition.inputs().get(0).type()).isEqualTo(SkillDefinition.ParameterType.STRING);
            assertThat(definition.inputs().get(1).name()).isEqualTo("amount");
            assertThat(definition.inputs().get(1).type()).isEqualTo(SkillDefinition.ParameterType.NUMBER);
        }

        @Test
        @DisplayName("描述为空时应使用方法名作为描述")
        void shouldUseMethodNameAsDescription_whenDescriptionEmpty() throws Exception {
            Method method = SingleSkillService.class.getDeclaredMethod("simple");
            Skill annotation = method.getAnnotation(Skill.class);

            SkillDefinition definition = registrar.createDefinition(annotation, method);

            assertThat(definition.description()).isEqualTo("simple");
        }

        @Test
        @DisplayName("应将 beanMethod 信息存入 metadata")
        void shouldStoreBeanMethodInMetadata() throws Exception {
            Method method = TestSkillService.class.getDeclaredMethod("queryUser", String.class);
            Skill annotation = method.getAnnotation(Skill.class);

            SkillDefinition definition = registrar.createDefinition(annotation, method);

            assertThat(definition.metadata()).isNotNull();
            assertThat((String) definition.metadata().get("beanMethod"))
                    .contains("TestSkillService")
                    .contains("queryUser");
        }
    }
}
