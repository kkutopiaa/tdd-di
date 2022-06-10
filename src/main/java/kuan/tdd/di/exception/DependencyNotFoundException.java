package kuan.tdd.di.exception;

/**
 * @author qinxuekuan
 * @date 2022/6/8
 */
public class DependencyNotFoundException extends RuntimeException {

    private final Class<?> component;
    private final Class<?> dependency;

    public DependencyNotFoundException(Class<?> component, Class<?> dependency) {
        this.component = component;
        this.dependency = dependency;
    }

    public Class<?> getDependency() {
        return dependency;
    }

    public Class<?> getComponent() {
        return component;
    }
}
