package com.example.inventory;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * README 6.1에 적은 의존 규칙을 테스트로 고정한다. 문서와 코드가 어긋나면 이 테스트가 먼저 실패한다.
 * 운영 코드만 검사한다. 테스트 코드는 여러 계층을 함께 다루므로 대상에서 뺀다.
 */
class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.inventory");

    @Test
    void 계층은_in_app_domain_out_방향으로만_의존한다() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("in").definedBy("..in..")
                .layer("app").definedBy("..app..")
                .layer("domain").definedBy("..domain..")
                .layer("out").definedBy("..out..")
                .layer("global").definedBy("..global..")

                .whereLayer("in").mayNotBeAccessedByAnyLayer()
                // 전역 핸들러는 유스케이스 예외(멱등 키 충돌)를 HTTP 응답으로 바꾸기 위해 app을 안다
                .whereLayer("app").mayOnlyBeAccessedByLayers("in", "global")
                .whereLayer("domain").mayOnlyBeAccessedByLayers("app", "out", "global")
                .whereLayer("out").mayOnlyBeAccessedByLayers("app")
                // 에러 코드와 HTTP 상태는 global 안에서만 쓴다. 도메인과 유스케이스는 global을 모른다
                .whereLayer("global").mayNotBeAccessedByAnyLayer()
                .check(CLASSES);
    }

    @Test
    void 도메인은_HTTP_트랜잭션_리포지토리를_모른다() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.http..",
                        "org.springframework.web..",
                        "org.springframework.transaction..",
                        "org.springframework.data.repository..",
                        "org.springframework.data.jpa.repository..")
                .check(CLASSES);
    }

    @Test
    void 상품은_재고를_모른다() {
        noClasses().that().resideInAPackage("com.example.inventory.product..")
                .should().dependOnClassesThat().resideInAPackage("com.example.inventory.stock..")
                .check(CLASSES);
    }

    @Test
    void 기능_패키지_사이에_순환_의존이_없다() {
        slices().matching("com.example.inventory.(*)..")
                .should().beFreeOfCycles()
                .check(CLASSES);
    }

    @Test
    void 컨트롤러는_in_계층에만_둔다() {
        classes().that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..in..")
                .check(CLASSES);
    }

    @Test
    void 리포지토리는_out_계층에만_둔다() {
        classes().that().areAssignableTo(Repository.class)
                .should().resideInAPackage("..out..")
                .check(CLASSES);
    }
}
