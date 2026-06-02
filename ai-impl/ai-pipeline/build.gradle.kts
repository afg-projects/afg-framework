plugins { `java-library` }

dependencies {
    api(project(":ai-core"))
    api(libs.spring.boot.autoconfigure)
    api(libs.reactor.core)
    api(libs.jspecify)
    // For RestKnowledgeSearchClient
    api(libs.spring.boot.starter.web)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("io.projectreactor:reactor-test")
}
