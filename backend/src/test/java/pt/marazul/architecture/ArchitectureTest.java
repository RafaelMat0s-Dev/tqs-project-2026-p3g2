package pt.marazul.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "pt.marazul", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    static final String ROOT = "pt.marazul.";
    static final List<String> MODULES = List.of(
            "identity", "contract", "network", "fleet", "telemetry", "booking",
            "autonomy", "trip", "payment", "maintenance", "metrics", "gbfs");

    @ArchTest
    static final ArchRule domainIsPlainJava = noClasses()
            .that().resideInAPackage("pt.marazul..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "javax..", "org.hibernate..", "com.fasterxml.jackson..", "tools.jackson..")
            .because("business rules are framework-independent Java (AGENTS.md, section 4.4 rule 1)");

    @ArchTest
    static final ArchRule domainDependsOnNoOuterLayer = noClasses()
            .that().resideInAPackage("pt.marazul..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "pt.marazul..application..", "pt.marazul..api..", "pt.marazul..infrastructure..")
            .because("the domain is the inner layer of each module");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage("pt.marazul..application..")
            .should().dependOnClassesThat().resideInAnyPackage("pt.marazul..api..", "pt.marazul..infrastructure..")
            .because("api and infrastructure depend on application, never the reverse (section 4.4 rule 2)");

    @ArchTest
    static final ArchRule domainNeverReadsTheSystemClock = noClasses()
            .that().resideInAPackage("pt.marazul..domain..")
            .should().callMethodWhere(DescribedPredicate.describe("java.time *.now()",
                    (JavaMethodCall call) -> call.getName().equals("now")
                            && call.getTargetOwner().getPackageName().equals("java.time")))
            .because("time-dependent rules receive an injected Clock (AD7, BR5, BR6, BR13, BR15)");

    @ArchTest
    static final ArchRule sharedDependsOnNoBusinessModule = noClasses()
            .that().resideInAPackage("pt.marazul.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    MODULES.stream().map(module -> ROOT + module + "..").toArray(String[]::new));

    @ArchTest
    static final ArchRule modulesAreFreeOfCycles = slices()
            .matching("pt.marazul.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule modulesTalkOnlyThroughTheirApi = classes()
            .should(onlyUseOtherModulesThroughTheirApi())
            .because("a module only uses another through its public facade or its events (section 4.4 rule 3)");

    @ArchTest
    static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule noStandardStreams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    static ArchCondition<JavaClass> onlyUseOtherModulesThroughTheirApi() {
        return new ArchCondition<>("only use other modules through their api package") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String from = moduleOf(origin.getPackageName());
                if (from == null) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    String to = moduleOf(targetPackage);
                    boolean otherModule = to != null && !to.equals(from) && !to.equals("shared");
                    if (otherModule && !targetPackage.startsWith(ROOT + to + ".api")) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    static String moduleOf(String packageName) {
        if (!packageName.startsWith(ROOT)) {
            return null;
        }
        String rest = packageName.substring(ROOT.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
