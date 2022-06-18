package kuan.tdd.di;

import jakarta.inject.Inject;
import kuan.tdd.di.exception.CyclicDependenciesFoundException;
import kuan.tdd.di.exception.DependencyNotFoundException;
import kuan.tdd.di.exception.IllegalComponentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;

/**
 * @author qinxuekuan
 * @date 2022/6/2
 */
public class ContainerTest {

    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }


    @Nested
    public class ComponentConstruction {
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };

            config.bind(Component.class, instance);

            assertSame(instance, config.getContext().get(Component.class).get());
        }

        // TODO abstract class

        // TODO interface


        @Test
        public void should_return_empty_if_component_not_defined() {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

        @Nested
        public class ConstructorInjection {
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                config.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = config.getContext().get(Component.class).get();

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };

                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, dependency);
                Component instance = config.getContext().get(Component.class).get();

                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);
                config.bind(String.class, "indirect dependency");

                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);

                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertNotNull(dependency);

                assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> config.bind(Component.class, ComponentWithMultiInjectConstructors.class));
            }

            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> config.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class));
            }

            @Test
            public void should_throw_exception_if_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);

                DependencyNotFoundException exception =
                        assertThrows(DependencyNotFoundException.class, () -> config.getContext());
                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());
            }

            @Test
            public void should_throw_exception_if_transitive_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);

                DependencyNotFoundException exception =
                        assertThrows(DependencyNotFoundException.class, () -> config.getContext());
                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());
            }


            @Test
            public void should_throw_exception_if_cyclic_dependencies_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesFoundException exception =
                        assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

                List<Class<?>> components = List.of(exception.getComponents());
                assertEquals(2, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
            }

            @Test
            public void should_throw_exception_if_transitive_cyclic_dependencies_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

                CyclicDependenciesFoundException exception =
                        assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

                List<Class<?>> components = List.of(exception.getComponents());
                assertEquals(3, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherDependency.class));
            }

        }

        @Nested
        public class FieldInjection {

            class ComponentWithFieldInjection{
                @Inject
                private Dependency dependency;

                public Dependency getDependency() {
                    return dependency;
                }
            }

            // 基于之前重构之后的代码，测试方式有多种选择。
            // 第一种测试方式： 集成测试（严格意义上的）
            // 还是在 ContextConfig 这个功能上下文中进行测试，测试粒度会大一些。
            @Test
            @Disabled
            public void should_inject_dependency_via_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);

                ComponentWithFieldInjection component = config.getContext().get(ComponentWithFieldInjection.class).get();

                assertSame(dependency, component.getDependency());
            }

            // 第二种测试方式： 单元测试（严格意义上的）
            // 在拆解出的 ConstructorInjectionProvider 这个功能上下文中进行测试，，测试粒度会小一些。
            @Test
            @Disabled
            public void should_create_component_with_injection_field() {
                Context context = Mockito.mock(Context.class);
                Dependency dependency = Mockito.mock(Dependency.class);
                Mockito.when(context.get(eq(Dependency.class)))
                        .thenReturn(Optional.of(dependency));

                ConstructorInjectionProvider<ComponentWithFieldInjection> provider =
                        new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
                ComponentWithFieldInjection component = provider.get(context);

                assertSame(dependency, component.getDependency());
            }


            // provider dependency information for field injection

            // 依赖找不到的情况
            @Test
            @Disabled
            public void should_throw_exception_when_field_dependency_missing() {
                config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);

                assertThrows(DependencyNotFoundException.class, () -> config.getContext());
            }

            // 依赖找不到的情况。 对 ConstructorInjectionProvider 进行测试。
            @Test
            @Disabled
            public void should_include_field_dependency_in_dependencies_for_not_found() {
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider =
                        new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);

                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

            class DependencyWithFieldInjection implements Dependency {
                @Inject
                ComponentWithFieldInjection component;
            }

            // 循环依赖的情况
            @Test
            @Disabled
            public void should_throw_exception_when_field_has_cyclic_dependencies() {
                config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);
                config.bind(Dependency.class, DependencyWithFieldInjection.class);

                assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
            }

            // 循环依赖的情况， 对 ConstructorInjectionProvider 进行测试。
            // 和依赖找不到的测试是完全一样的。
            // 因为 ConstructorInjectionProvider 只是提供信息，并没有做实际功能，实际的功能都是在 ContextConfig 里去完成的。
            // 只要你提供了相应的组件信息，交给 ContextConfig， 如果有依赖找不到，循环依赖的情况，ContextConfig 就会给你报错。
            @Test
            @Disabled
            public void should_include_field_dependency_in_dependencies_for_cyclic() {
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider =
                        new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);

                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }


        }

        @Nested
        public class MethodInjection {

        }

    }


    @Nested
    public class DependenciesSelection {

    }

    @Nested
    public class LifecycleManagement {

    }


}


interface Component {

}

interface Dependency {

}

interface AnotherDependency {

}

// inner class , static or non-static , default constructor , has some trouble...
class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
}

class ComponentWithInjectConstructor implements Component {
    Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectConstructors implements Component {
    @Inject
    public ComponentWithMultiInjectConstructors(String name, Double value) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }
}

class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {
    public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
    }
}

class DependencyWithInjectConstructor implements Dependency {
    String dependency;

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}

class DependencyDependedOnComponent implements Dependency {
    private Component component;

    @Inject
    public DependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}

class DependencyDependedOnAnotherDependency implements Dependency {
    private AnotherDependency anotherDependency;

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}

class AnotherDependencyDependedOnComponent implements AnotherDependency {

    private Component component;

    @Inject
    public AnotherDependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}

