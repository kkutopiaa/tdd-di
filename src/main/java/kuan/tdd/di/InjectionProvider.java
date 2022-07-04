package kuan.tdd.di;

import jakarta.inject.Inject;
import kuan.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

/**
 * @author qinxuekuan
 * @date 2022/6/17
 */
class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private final List<Field> injectFields;
    private final List<Method> injectMethods;

    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }
        injectConstructor = getInjectConstructor(component);
        injectFields = getInjectFields(component);
        injectMethods = getInjectMethods(component);

        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
    }

    static private <T> List<Method> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = traverse(component,
                (methods, current) -> injectable(current.getDeclaredMethods())
                        .filter(m -> isOverrideByInjectMethod(methods, m))
                        .filter(m -> isOverrideByNoInjectMethod(component, m))
                        .toList()
        );
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    static private <T> List<Field> getInjectFields(Class<T> component) {
        return traverse(component, (fields, current) -> injectable(current.getDeclaredFields()).toList());
    }

    static private <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }

        return (Constructor<Type>) injectConstructors.stream().findFirst()
                .orElseGet(() -> defaultConstructor(implementation));
    }

    @Override
    public T get(Context context) {
        try {
            T instance = injectConstructor.newInstance(toDependencies(context, injectConstructor));
            for (Field field : injectFields) {
                field.setAccessible(true);
                field.set(instance, toDependency(context, field));
            }
            for (Method method : injectMethods) {
                method.invoke(instance, toDependencies(context, method));
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Stream.concat(Stream.concat(stream(injectConstructor.getParameterTypes()),
                                injectFields.stream().map(Field::getType)),
                        injectMethods.stream().flatMap(m -> stream(m.getParameterTypes())))
                .toList();
    }

    @Override
    public List<Type> getDependencyTypes() {
        return Stream.concat(Stream.concat(stream(injectConstructor.getParameters()).map(Parameter::getParameterizedType),
                                injectFields.stream().map(Field::getGenericType)),
                        injectMethods.stream().flatMap(m -> stream(m.getParameters()).map(Parameter::getParameterizedType)))
                .toList();
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

    private static Object toDependency(Context context, Field field) {
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            return context.get((ParameterizedType) type).get();
        }
        return context.get((Class<?>) type).get();
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return stream(executable.getParameters()).map(
                p -> {
                    Type type = p.getParameterizedType();
                    if (type instanceof ParameterizedType) {
                        return context.get((ParameterizedType) type).get();
                    }
                    return context.get((Class<?>) type).get();
                }
        ).toArray(Object[]::new);
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
