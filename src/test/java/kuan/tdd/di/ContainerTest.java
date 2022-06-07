package kuan.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author qinxuekuan
 * @date 2022/6/2
 */
public class ContainerTest {

    Context context;

    @BeforeEach
    public void setup() {
        context = new Context();
    }


    @Nested
    public class ComponentConstruction {
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };

            context.bind(Component.class, instance);

            assertSame(instance, context.get(Component.class));

        }

        // TODO abstract class

        // TODO interface


        @Nested
        public class ConstructorInjection {
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = context.get(Component.class);

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            // TODO with dependencies
            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };

                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, dependency);
                Component instance = context.get(Component.class);

                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }


            // TODO A --> B --> C

        }

        @Nested
        public class FieldInjection {

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