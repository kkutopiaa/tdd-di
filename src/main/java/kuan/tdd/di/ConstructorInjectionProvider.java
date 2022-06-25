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
class ConstructorInjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private final List<Field> injectFields;
    private final List<Method> injectMethods;

    public ConstructorInjectionProvider(Class<T> component) {
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
        List<Method> injectMethods = new ArrayList<>();
        BiFunction<List<Method>, Class<?>, List<Method>> function = (methods, current) -> getC(component, methods, current);
        Class<?> current = component;
        while (current != Object.class) {
            injectMethods.addAll(
                    function.apply(injectMethods, current)
            );
            current = current.getSuperclass();
        }
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> List<Method> getC(Class<T> component, List<Method> injectMethods, Class<?> current) {
        return injectable(current.getDeclaredMethods())
                .filter(m -> isOverrideByInjectMethod(injectMethods, m))
                .filter(m -> isOverrideByNoInjectMethod(component, m))
                .toList();
    }


    static private <T> List<Field> getInjectFields(Class<T> component) {
        List<Field> injectFields = new ArrayList<>();
        BiFunction<List<Field>, Class<?>, List<Field>> function = (fields, current) -> getC(fields, current);
        Class<?> current = component;
        while (current != Object.class) {
            injectFields.addAll(
                    function.apply(injectFields, current)
            );
            current = current.getSuperclass();
        }
        return injectFields;
    }

    private static List<Field> getC(List<Field> fields, Class<?> current) {
        return injectable(current.getDeclaredFields()).toList();
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



    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields).filter(f -> f.isAnnotationPresent(Inject.class));
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
        return context.get(field.getType()).get();
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return stream(executable.getParameterTypes())
                .map(t -> context.get(t).get())
                .toArray(Object[]::new);
    }

    private static <Type> Constructor<Type> defaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }

}
