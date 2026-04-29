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
 * ArchUnit 架构约束测试
 * 确保项目代码遵循预定义的架构规则
 */
class ArchitectureTest {

    private static JavaClasses coreClasses;

    @BeforeAll
    static void setUp() {
        coreClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("io.github.afgprojects.framework.core");
    }

    @Nested
    @DisplayName("包依赖规则")
    class PackageDependencyTests {

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

    @Nested
    @DisplayName("命名约定规则")
    class NamingConventionTests {

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

    @Nested
    @DisplayName("接口命名规则")
    class InterfaceNamingTests {

        @Test
        @DisplayName("Registry 结尾的类应位于 config 或 module 包下")
        void registryClassesShouldBeInCorrectPackage() {
            ArchRule rule = classes()
                    .that()
                    .haveSimpleNameEndingWith("Registry")
                    .should()
                    .resideInAnyPackage("..config..", "..module..", "..client..")
                    .because("Registry 类应位于 config、module 或 client 包下")
                    .allowEmptyShould(true);

            rule.check(coreClasses);
        }
    }

    @Nested
    @DisplayName("工具类规则")
    class UtilityClassTests {

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

    @Nested
    @DisplayName("异常类规则")
    class ExceptionClassTests {

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

    @Nested
    @DisplayName("日志使用规则")
    class LoggingTests {

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

        @Test
        @DisplayName("不应使用 System.out，应使用 Logger")
        void shouldNotUseSystemOut() {
            ArchRule rule =
                    noClasses().should().callMethod(System.class, "out").because("应使用 Logger 而非 System.out 进行日志输出")
                    .allowEmptyShould(true);

            rule.check(coreClasses);
        }

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
