package kuan.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import kuan.tdd.di.exception.DependencyNotFoundException;
import kuan.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author qinxuekuan
 * @date 2022/6/6
 */
public class Context {

    private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (Provider<Type>) () -> instance);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<Implementation> injectConstructor = getConstructor(implementation);

        providers.put(type, (Provider<Type>) () -> {
            try {
                Object[] dependencies = Arrays.stream(injectConstructor.getParameters())
                        .map(p -> get(p.getType()).orElseThrow(DependencyNotFoundException::new))
                        .toArray(Object[]::new);
                return (Type) injectConstructor.newInstance(dependencies);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <Type> Constructor<Type> getConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = Arrays.stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class)).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }

        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });


    }

    public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get());
    }

}
