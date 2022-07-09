package kuan.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * @author qinxuekuan
 * @date 2022/7/9
 */
public
class ComponentRef<ComponentType> {
    private Type container;

    private Component component;

    ComponentRef(Type type, Annotation qualifier) {
        init(type, qualifier);
    }

    ComponentRef(Class<ComponentType> componentType) {
        init(componentType, null);
    }

    public static ComponentRef of(Type type) {
        return new ComponentRef(type, null);
    }

    public static ComponentRef of(Type type, Annotation qualifier) {
        return new ComponentRef(type, qualifier);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
        return new ComponentRef(component);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
        return new ComponentRef(component, qualifier);
    }

    protected ComponentRef() {
        Type type = ((ParameterizedType) (getClass().getGenericSuperclass())).getActualTypeArguments()[0];
        init(type, null);
    }

    private void init(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType container) {
            this.container = container.getRawType();
            this.component = new Component((Class<?>) container.getActualTypeArguments()[0], qualifier);
        } else {
            this.component = new Component((Class<?>) type, qualifier);
        }
    }

    public Type getContainer() {
        return container;
    }

    public Class<?> getComponent() {
        return component.type();
    }

    public Annotation getQualifier() {
        return component.qualifier();
    }

    public boolean isContainer() {
        return this.container != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentRef<?> that = (ComponentRef<?>) o;
        return Objects.equals(container, that.container) && component.equals(that.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, component);
    }
}
