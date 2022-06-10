package kuan.tdd.di.exception;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author qinxuekuan
 * @date 2022/6/8
 */
public class CyclicDependenciesFoundException extends RuntimeException {

    private final Set<Class<?>> components = new HashSet<>();

    public CyclicDependenciesFoundException(Class<?> componentType) {
        components.add(componentType);
    }

    public CyclicDependenciesFoundException(Class<?> componentType, CyclicDependenciesFoundException e) {
        components.add(componentType);
        components.addAll(e.components);
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class<?>[]::new);
    }

}
