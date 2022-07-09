package kuan.tdd.di.exception;

import kuan.tdd.di.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author qinxuekuan
 * @date 2022/6/8
 */
public class CyclicDependenciesFoundException extends RuntimeException {

    private final Set<Component> components = new HashSet<>();

    public CyclicDependenciesFoundException(List<Component> visiting) {
        components.addAll(visiting);
    }

    public Class<?>[] getComponents() {
        return components.stream().map(Component::type).toArray(Class<?>[]::new);
    }

}
