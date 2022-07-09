package kuan.tdd.di.exception;

import kuan.tdd.di.Component;

/**
 * @author qinxuekuan
 * @date 2022/6/8
 */
public class DependencyNotFoundException extends RuntimeException {

    private final Component component;

    private final Component dependency;

    public DependencyNotFoundException(Component component, Component dependency) {
        this.component = component;
        this.dependency = dependency;
    }

    public Component getComponent() {
        return this.component;
    }

    public Component getDependency() {
        return this.dependency;
    }

}
