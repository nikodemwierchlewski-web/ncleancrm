package pl.nclean.crm.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** Runs in the normal Maven test/CI phase; no exclusion list for existing feature cycles. */
class ArchitectureTest {
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("pl.nclean.crm");
    private static final DescribedPredicate<JavaClass> BUSINESS = new DescribedPredicate<>(
            "business classes, excluding HTTP presentation and application configuration") {
        public boolean test(JavaClass type) {
            return !type.getSimpleName().endsWith("Controller")
                    && !type.getPackageName().startsWith("pl.nclean.crm.ui")
                    && !type.getPackageName().startsWith("pl.nclean.crm.security");
        }
    };

    @Test void controllersUseApplicationServicesInsteadOfDatabaseAccess() {
        noClasses().that().haveSimpleNameEndingWith("Controller").should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework.jdbc..", "javax.sql..", "java.sql..",
                        "org.hibernate..", "org.springframework.data.repository..")
                .check(CLASSES);
        noClasses().that().haveSimpleNameEndingWith("Controller").should().dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository").check(CLASSES);
        // Persistence exceptions may be translated to HTTP; entity managers are not web adapters.
        noClasses().that().haveSimpleNameEndingWith("Controller").should().dependOnClassesThat()
                .haveFullyQualifiedName("jakarta.persistence.EntityManager").check(CLASSES);
        noClasses().that().haveSimpleNameEndingWith("Controller").should().dependOnClassesThat()
                .haveFullyQualifiedName("jakarta.persistence.EntityManagerFactory").check(CLASSES);
    }

    @Test void transactionBoundariesDoNotBelongInControllers() {
        noClasses().that().haveSimpleNameEndingWith("Controller").should()
                .beAnnotatedWith(Transactional.class).check(CLASSES);
        noMethods().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
                .should().beAnnotatedWith(Transactional.class).check(CLASSES);
    }

    @Test void sharedCodeDoesNotDependOnFeatureModules() {
        classes().that().resideInAPackage("pl.nclean.crm.shared..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "pl.nclean.crm.shared..", "java..", "org.springframework..", "com.fasterxml.jackson.annotation..")
                .check(CLASSES);
    }

    @Test void businessLogicNeverCallsAnHttpController() {
        noClasses().that(BUSINESS).should().dependOnClassesThat()
                .haveSimpleNameEndingWith("Controller").check(CLASSES);
    }

    @Test void featureBusinessDependenciesAreAcyclic() {
        // Controllers can compose several read models; they are adapters, not domain dependencies.
        slices().matching("pl.nclean.crm.(*)..").should().beFreeOfCycles()
                .check(CLASSES.that(BUSINESS));
    }

    @Test void dashboardDoesNotBecomeAGeneralPurposeUtilityModule() {
        noClasses().that(BUSINESS).and().resideOutsideOfPackage("pl.nclean.crm.dashboard..")
                .should().dependOnClassesThat().resideInAPackage("pl.nclean.crm.dashboard..")
                .check(CLASSES);
    }

    @Test void removalCoordinatorDependsOnlyOnItsContract() {
        classes().that().resideInAPackage("pl.nclean.crm.removal..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "pl.nclean.crm.removal..", "pl.nclean.crm.shared.removal..",
                        "java..", "org.springframework..")
                .check(CLASSES);
    }
}
