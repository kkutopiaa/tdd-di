package kuan.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import kuan.tdd.di.exception.IllegalComponentException;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

/**
 * @author qinxuekuan
 * @date 2022/6/17
 */
class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {

    private final Injectable<Constructor<T>> injectConstructor;

    private final List<Injectable<Method>> injectMethods;

    private final List<Injectable<Field>> injectFields;


    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }

        this.injectConstructor = getInjectConstructor(component);
        this.injectMethods = getInjectMethods(component);
        this.injectFields = getInjectFields(component);

        if (injectFields.stream().map(Injectable::element).anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().map(Injectable::element).anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
    }

    private Injectable<Constructor<T>> getInjectConstructor(Class<T> component) {
        List<Constructor<?>> injectConstructors = injectable(component.getConstructors()).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return Injectable.of((Constructor<T>) injectConstructors.stream().findFirst()
                .orElseGet(() -> defaultConstructor(component)));
    }

    private static List<Injectable<Field>> getInjectFields(Class<?> component) {
        List<Field> injectFields = InjectionProvider.traverse(component,
                (fields, current) -> injectable(current.getDeclaredFields()).toList());
        return injectFields.stream().map(Injectable::of).toList();
    }

    private static List<Injectable<Method>> getInjectMethods(Class<?> component) {
        List<Method> injectMethods = traverse(component,
                (methods, current) -> injectable(current.getDeclaredMethods())
                        .filter(m -> isOverrideByInjectMethod(methods, m))
                        .filter(m -> isOverrideByNoInjectMethod(component, m))
                        .toList()
        );
        Collections.reverse(injectMethods);
        return injectMethods.stream().map(Injectable::of).toList();
    }

    @Override
    public T get(Context context) {
        try {
            T instance = injectConstructor.element().newInstance(injectConstructor.toDependency(context));
            for (Injectable<Field> field : injectFields) {
                field.element().setAccessible(true);
                field.element().set(instance, field.toDependency(context)[0]);
            }
            for (Injectable<Method> method : injectMethods) {
                method.element().invoke(instance, method.toDependency(context));
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public List<ComponentRef<?>> getDependencies() {
        return Stream.concat(Stream.concat(
                                stream(injectConstructor.required()),
                                injectFields.stream().flatMap(f -> stream(f.required()))
                        ),
                        injectMethods.stream().flatMap(m -> stream(m.required())))
                .toList();
    }

    static record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] required) {

        static <Element extends Executable> Injectable<Element> of(Element element) {
            ComponentRef<?>[] required = stream(element.getParameters()).map(InjectionProvider::toComponentRef).toArray(ComponentRef<?>[]::new);
            return new Injectable<>(element, required);
        }

        static Injectable<Field> of(Field field) {
            return new Injectable<>(field, new ComponentRef<?>[]{toComponentRef(field)});
        }

        Object[] toDependency(Context context) {
            return stream(required).map(context::get).map(Optional::get).toArray();
        }

    }


    private static <T extends AnnotatedElement> Stream<T> injectable(T[] members) {
        return stream(members).filter(m -> m.isAnnotationPresent(Inject.class));
    }

    private static boolean isOverrideByInjectMethod(List<Method> injectMethods, Method m) {
        return injectMethods.stream().noneMatch(subMethod -> isOverrideMethod(m, subMethod));
    }

    private static <T> boolean isOverrideByNoInjectMethod(Class<T> component, Method m) {
        return stream(component.getDeclaredMethods())
                .filter(methodInSub -> !methodInSub.isAnnotationPresent(Inject.class))
                .noneMatch(methodInSub -> isOverrideMethod(m, methodInSub));
    }

    private static boolean isOverrideMethod(Method m, Method subMethod) {
        return subMethod.getName().equals(m.getName()) && Arrays.equals(subMethod.getParameterTypes(), m.getParameterTypes());
    }

    private static ComponentRef toComponentRef(Field field) {
        Annotation qualifier = getQualifier(field);
        return ComponentRef.of(field.getGenericType(), qualifier);
    }

    private static ComponentRef toComponentRef(Parameter parameter) {
        Annotation qualifier = getQualifier(parameter);
        return ComponentRef.of(parameter.getParameterizedType(), qualifier);
    }

    private static Annotation getQualifier(AnnotatedElement element) {
        List<Annotation> qualifiers = stream(element.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .toList();
        if (qualifiers.size() > 1) {
            throw new IllegalComponentException();
        }

        return qualifiers.stream().findFirst().orElse(null);
    }


    private static <Type> Constructor<Type> defaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }

    private static <T> List<T> traverse(Class<?> component, BiFunction<List<T>, Class<?>, List<T>> finder) {
        List<T> members = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            members.addAll(
                    finder.apply(members, current)
            );
            current = current.getSuperclass();
        }
        return members;
    }

}
