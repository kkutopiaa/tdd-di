package kuan.tdd.di;

import jakarta.inject.Provider;
import kuan.tdd.di.exception.CyclicDependenciesFoundException;
import kuan.tdd.di.exception.DependencyNotFoundException;

import java.lang.reflect.ParameterizedType;
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
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type))
                        .map(provider -> (Type) provider.get(this));
            }

            @Override
            public Optional get(ParameterizedType type) {
                Class<?> componentType = (Class<?>) type.getActualTypeArguments()[0];
                return Optional.ofNullable(providers.get(componentType))
                        .map(componentProvider -> (Provider<Object>) () -> componentProvider.get(this));
            }

        };
    }

    public void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Class<?> dependency : providers.get(component).getDependencies()) {
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

    }

    interface ComponentProvider<T> {
        // ConstructorInjectionProvider 需要有 context 这个上下文（并不是每次去 getContext 的时候都是一个新的 Context），但 ContextConfig 又提供不了。
        // 用这个接口提供 Context。 代表在传入的 Context 上下文中，获取 T 对象。
        T get(Context context);

        default List<Class<?>> getDependencies(){
            return List.of();
        }
    }


}
