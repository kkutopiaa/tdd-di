package kuan.tdd.di;

import jakarta.inject.Provider;
import kuan.tdd.di.exception.CyclicDependenciesFoundException;
import kuan.tdd.di.exception.DependencyNotFoundException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author qinxuekuan
 * @date 2022/6/6
 */
public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (ComponentProvider<Type>) context -> instance);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        // 检查是否存在依赖
        // 检查是否发生了循环依赖
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {
            private <Type> Optional<Type> getComponent(Class<Type> type) {
                return Optional.ofNullable(providers.get(type))
                        .map(provider -> (Type) provider.get(this));
            }

            private Optional getContainer(ParameterizedType type) {
                if (type.getRawType() != Provider.class) {
                    return Optional.empty();
                }
                return Optional.ofNullable(providers.get(getComponentType(type)))
                        .map(componentProvider -> (Provider<Object>) () -> componentProvider.get(this));
            }

            @Override
            public Optional get(Type type) {
                if (isContainerType(type)) {
                    return getContainer((ParameterizedType) type);
                }
                return getComponent((Class<?>) type);
            }
        };
    }

    private Class<?> getComponentType(Type type) {
        return (Class<?>) ((ParameterizedType)type).getActualTypeArguments()[0];
    }

    private boolean isContainerType(Type type) {
        return type instanceof ParameterizedType;
    }

    public void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {
            if (isContainerType(dependency)) {
                checkContainerTypeDependency(component, dependency);
            } else {
                checkComponentDependency(component, visiting, (Class<?>) dependency);
            }

        }

    }

    private void checkContainerTypeDependency(Class<?> component, Type dependency) {
        if (!providers.containsKey(getComponentType(dependency))) {
            throw new DependencyNotFoundException(component, getComponentType(dependency));
        }
    }

    private void checkComponentDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        if (!providers.containsKey(dependency)) {
            throw new DependencyNotFoundException(component, dependency);
        }
        if (visiting.contains(dependency)) {
            throw new CyclicDependenciesFoundException(visiting);
        }
        visiting.push(dependency);
        checkDependencies(dependency, visiting);
        visiting.pop();
    }

    interface ComponentProvider<T> {
        // ConstructorInjectionProvider 需要有 context 这个上下文（并不是每次去 getContext 的时候都是一个新的 Context），但 ContextConfig 又提供不了。
        // 用这个接口提供 Context。 代表在传入的 Context 上下文中，获取 T 对象。
        T get(Context context);

        default List<Type> getDependencies(){
            return List.of();
        }

    }


}
