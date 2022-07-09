package kuan.tdd.di.exception;

import kuan.tdd.di.Component;

/**
 * @author qinxuekuan
 * @date 2022/6/8
 */
public class DependencyNotFoundException extends RuntimeException {

    private final Class<?> component;
    private final Class<?> dependency;

    private Component componentComponent;

    private Component dependencyComponent;

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

    public Component getComponentComponent() {
        return this.componentComponent;
    }

    public Component getDependencyComponent() {
        return this.dependencyComponent;
    }

}
