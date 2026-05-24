package io.github.afgprojects.framework.core.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * ArchUnit 架构约束测试。
 * <p>
 * 使用 ArchUnit 框架验证 core 模块的代码架构是否符合预定义的规则，包括：
 * <ul>
 *   <li>包依赖规则：确保分层架构的正确性</li>
 *   <li>命名约定规则：确保类名与包结构的一致性</li>
 *   <li>接口命名规则：确保 Registry 类的正确位置</li>
 *   <li>工具类规则：确保工具类的正确设计模式</li>
 *   <li>异常类规则：确保异常类的继承层次</li>
 *   <li>日志使用规则：确保使用正确的日志框架</li>
 * </ul>
 *
 * @see com.tngtech.archunit.lang.ArchRule ArchUnit 规则定义
 * @see com.tngtech.archunit.core.domain.JavaClasses ArchUnit 类导入
 */
class ArchitectureTest {

    private static JavaClasses coreClasses;

    /**
     * 初始化测试所需的类导入。
     * <p>
     * 导入 core 模块的所有类，排除测试类，用于后续架构规则检查。
     */
    @BeforeAll
    static void setUp() {
        coreClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("io.github.afgprojects.framework.core");
    }

    /**
     * 包依赖规则测试组。
     * <p>
     * 验证各层之间的依赖关系是否符合分层架构设计，防止跨层直接依赖内部实现。
     */
    @Nested
    @DisplayName("包依赖规则")
    class PackageDependencyTests {

        /**
         * 测试 web 层不应直接依赖 module 层的内部实现。
         * <p>
         * web 层应通过 module 层的公共 API 访问功能，而非直接依赖 exception 等内部包。
         */
        @Test
        @DisplayName("web 层不应直接依赖 module 层内部实现")
        void webShouldNotDependOnModuleInternals() {
            ArchRule rule = noClasses()
                    .that()
                    .resideInAPackage("..web..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..module.exception..")
                    .because("web 层应通过 module 层的公共 API 访问功能，而非直接依赖内部实现")
                    .allowEmptyShould(true);

            rule.check(coreClasses);
        }

        /**
         * 测试 web 层不应直接依赖 config 层的内部实现。
         * <p>
         * web 层应通过 config 层的公共 API 访问功能，而非直接依赖 exception 等内部包。
         */
        @Test
        @DisplayName("web 层不应直接依赖 config 层内部实现")
        void webShouldNotDependOnConfigInternals() {
            ArchRule rule = noClasses()
                    .that()
                    .resideInAPackage("..web..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..config.exception..")
                    .because("web 层应通过 config 层的公共 API 访问功能，而非直接依赖内部实现")
                    .allowEmptyShould(true);

            rule.check(coreClasses);
        }
    }

    /**
     * 命名约定规则测试组。
     * <p>
     * 验证类命名与包结构的一致性，确保类名后缀能正确反映其所属的架构层。
     */
    @Nested
    @DisplayName("命名约定规则")
    class NamingConventionTests {

        /**
         * 测试 Controller 结尾的类应位于 web 包下。
         * <p>
         * Controller 类作为 web 层的入口，应位于 web 包下以保持清晰的分层架构。
         */
        @Test
        @DisplayName("Controller 结尾的类应在 web 包下")
        void controllerClassesShouldBeInWebPackage() {
            ArchRule rule = classes()
                    .that()
                    .haveSimpleNameEndingWith("Controller")
                    .and()
                    .doNotHaveModifier(JavaModifier.ABSTRACT)
                    .and()
                    .haveSimpleNameNotContaining("Exception")
                    .should()
                    .resideInAPackage("..web..")
                    .because("Controller 类应位于 web 包下以保持清晰的分层架构")
                    .allowEmptyShould(true);

            rule.check(coreClasses);
        }

        /**
         * 测试 Filter 结尾的类应位于 web 包下。
         * <p>
         * Filter 类作为 web 层的过滤器组件，应位于 web 包下。
         */
        @Test
        @DisplayName("Filter 结尾的类应在 web 包下")
        void filterClassesShouldBeInWebPackage() {
            ArchRule rule = classes()
                    .that()
                    .haveSimpleNameEndingWith("Filter")
                    .should()
                    .resideInAPackage("..web..")
                    .because("Filter 类应位于 web 包下")
                    .allowEmptyShould(true);

            rule.check(coreClasses);
        }
    }

    /**
     * 接口命名规则测试组。
     * <p>
     * 验证特定后缀的类是否位于正确的包下，确保注册器类的位置符合规范。
     */
    @Nested
    @DisplayName("接口命名规则")
    class InterfaceNamingTests {

        /**
         * 测试 Registry 结尾的类应位于 config、module 或 client 包下。
         * <p>
         * Registry 类作为配置或模块注册器，应位于相应的包下以保持职责清晰。
         */
        @Test
        @DisplayName("Registry 结尾的类应位于 config、module、client、api.registry 或 invocation 包下")
        void registryClassesShouldBeInCorrectPackage() {
            ArchRule rule = classes()
                    .that()
                    .haveSimpleNameEndingWith("Registry")
                    .should()
                    .resideInAnyPackage("..config..", "..module..", "..client..", "..api.registry..", "..invocation..")
                    .because("Registry 类应位于 config、module、client、api.registry 或 invocation 包下")
                    .allowEmptyShould(true);

            rule.check(coreClasses);
        }
    }

    /**
     * 工具类规则测试组。
     * <p>
     * 验证工具类的设计模式是否正确，确保工具类无法被实例化和继承。
     */
    @Nested
    @DisplayName("工具类规则")
    class UtilityClassTests {

        /**
         * 测试 Utils 结尾的类应是 final 且有私有构造函数。
         * <p>
         * 工具类应为 final 且无法实例化，确保其仅作为静态工具方法集合，
         * 防止误用和继承扩展。
         */
        @Test
        @DisplayName("Utils 结尾的类应是 final 且有私有构造函数")
        void utilsClassesShouldBeFinalWithPrivateConstructor() {
            ArchCondition<JavaClass> beFinalWithPrivateConstructors =
                    new ArchCondition<JavaClass>("have only private constructors and be final") {
                        @Override
                        public void check(JavaClass item, ConditionEvents events) {
                            boolean hasOnlyPrivateConstructors = item.getConstructors().stream()
                                    .allMatch(constructor ->
                                            constructor.getModifiers().contains(JavaModifier.PRIVATE));
                            boolean isFinal = item.getModifiers().contains(JavaModifier.FINAL);
                            boolean satisfied = hasOnlyPrivateConstructors && isFinal;
                            events.add(new SimpleConditionEvent(item, satisfied, item.getDescription()));
                        }
                    };

            ArchRule rule = classes()
                    .that()
                    .haveSimpleNameEndingWith("Utils")
                    .should(beFinalWithPrivateConstructors)
                    .because("工具类应为 final 且无法实例化，确保其仅作为静态工具方法集合")
                    .allowEmptyShould(true);

            rule.check(coreClasses);
        }
    }

    /**
     * 异常类规则测试组。
     * <p>
     * 验证异常类的继承层次是否符合项目设计规范。
     */
    @Nested
    @DisplayName("异常类规则")
    class ExceptionClassTests {

        /**
         * 测试所有异常类应继承 RuntimeException 或其子类。
         * <p>
         * 项目采用非检查异常设计，所有自定义异常应继承 RuntimeException，
         * 避免强制调用方处理异常。
         */
        @Test
        @DisplayName("所有异常类应继承 RuntimeException 或其子类")
        void exceptionClassesShouldExtendRuntimeException() {
            ArchRule rule = classes()
                    .that()
                    .areAssignableTo(Exception.class)
                    .and()
                    .haveSimpleNameEndingWith("Exception")
                    .should()
                    .beAssignableTo(RuntimeException.class)
                    .because("项目采用非检查异常设计，所有异常应继承 RuntimeException")
                    .allowEmptyShould(true);

            rule.check(coreClasses);
        }
    }

    /**
     * 日志使用规则测试组。
     * <p>
     * 验证日志框架的使用是否符合规范，确保统一使用 SLF4J 作为日志门面。
     */
    @Nested
    @DisplayName("日志使用规则")
    class LoggingTests {

        /**
         * 测试不应使用 java.util.logging，应使用 SLF4J。
         * <p>
         * 项目统一使用 SLF4J 作为日志门面，避免直接使用 java.util.logging
         * 导致日志配置不一致。
         */
        @Test
        @DisplayName("不应使用 java.util.logging，应使用 SLF4J")
        void shouldNotUseJavaUtilLogging() {
            ArchRule rule = noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("java.util.logging..")
                    .because("应使用 SLF4J 作为日志门面，而非 java.util.logging")
                    .allowEmptyShould(true);

            rule.check(coreClasses);
        }

        /**
         * 测试不应使用 System.out，应使用 Logger。
         * <p>
         * 使用 System.out 输出日志无法统一管理和配置，应使用 Logger 进行日志输出。
         */
        @Test
        @DisplayName("不应使用 System.out，应使用 Logger")
        void shouldNotUseSystemOut() {
            ArchRule rule =
                    noClasses().should().callMethod(System.class, "out").because("应使用 Logger 而非 System.out 进行日志输出")
                    .allowEmptyShould(true);

            rule.check(coreClasses);
        }

        /**
         * 测试不应使用 System.err，应使用 Logger。
         * <p>
         * 使用 System.err 输出错误日志无法统一管理和配置，应使用 Logger 进行错误日志输出。
         */
        @Test
        @DisplayName("不应使用 System.err，应使用 Logger")
        void shouldNotUseSystemErr() {
            ArchRule rule =
                    noClasses().should().callMethod(System.class, "err").because("应使用 Logger 而非 System.err 进行错误日志输出")
                    .allowEmptyShould(true);

            rule.check(coreClasses);
        }
    }
}
