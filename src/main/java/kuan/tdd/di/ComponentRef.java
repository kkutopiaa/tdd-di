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
    private Class<?> component;
    private Annotation qualifier;

    ComponentRef(Type type, Annotation qualifier) {
        init(type);
        this.qualifier = qualifier;
    }

    ComponentRef(Class<ComponentType> component) {
        init(component);
    }

    public static ComponentRef of(Type type) {
        return new ComponentRef(type, null);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
        return new ComponentRef(component);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
        return new ComponentRef(component, qualifier);
    }

    protected ComponentRef() {
        Type type = ((ParameterizedType) (getClass().getGenericSuperclass())).getActualTypeArguments()[0];
        init(type);
    }

    private void init(Type type) {
        if (type instanceof ParameterizedType container) {
            this.container = container.getRawType();
            this.component = (Class<?>) container.getActualTypeArguments()[0];
        } else {
            this.component = (Class<?>) type;
        }
    }

    public Type getContainer() {
        return container;
    }

    public Class<?> getComponent() {
        return component;
    }

    public Annotation getQualifier() {
        return qualifier;
    }

    public boolean isContainer() {
        return this.container != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentRef ref = (ComponentRef) o;
        return Objects.equals(container, ref.container) && component.equals(ref.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, component);
    }
}
