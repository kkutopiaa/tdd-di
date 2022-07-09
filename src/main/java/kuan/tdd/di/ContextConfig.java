package kuan.tdd.di;

import jakarta.inject.Provider;
import kuan.tdd.di.exception.CyclicDependenciesFoundException;
import kuan.tdd.di.exception.DependencyNotFoundException;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author qinxuekuan
 * @date 2022/6/6
 */
public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
    private Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        providers.put(type, (ComponentProvider<T>) context -> instance);
    }

    public <T> void bind(Class<T> type, T instance, Annotation qualifier) {
        components.put(new Component(type, qualifier), context -> instance);
    }

    record Component(Class<?> type, Annotation qualifier) {
    }


    public <T, Implementation extends T> void bind(Class<T> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public <T, Implementation extends T> void bind(Class<T> type, Class<Implementation> implementation, Annotation qualifier) {
    }

    public Context getContext() {
        // 检查是否存在依赖
        // 检查是否发生了循环依赖
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {

            @Override
            public <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref) {
                if (ref.getQualifier() != null) {
                    return Optional.ofNullable(components.get(new Component(ref.getComponent(), ref.getQualifier())))
                            .map(provider -> (ComponentType) provider.get(this));
                }

                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) {
                        return Optional.empty();
                    }
                    return (Optional<ComponentType>) Optional.ofNullable(providers.get(ref.getComponent()))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponent()))
                        .map(provider -> (ComponentType) provider.get(this));
            }
        };
    }

    public void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Context.Ref ref : providers.get(component).getDependencies()) {
            if (!providers.containsKey(ref.getComponent())) {
                throw new DependencyNotFoundException(component, ref.getComponent());
            }
            if (!ref.isContainer()) {
                if (!providers.containsKey(ref.getComponent())) {
                    throw new DependencyNotFoundException(component, ref.getComponent());
                }
                if (visiting.contains(ref.getComponent())) {
                    throw new CyclicDependenciesFoundException(visiting);
                }
                visiting.push(ref.getComponent());
                checkDependencies(ref.getComponent(), visiting);
                visiting.pop();
            }
        }
    }

    interface ComponentProvider<T> {
        // ConstructorInjectionProvider 需要有 context 这个上下文（并不是每次去 getContext 的时候都是一个新的 Context），但 ContextConfig 又提供不了。
        // 用这个接口提供 Context。 代表在传入的 Context 上下文中，获取 T 对象。
        T get(Context context);


        // 期望得到这样的一个方法： List<Ref> getDependencies()，  Ref 是对 Class 和 ParameterizedType 的封装
        default List<Context.Ref> getDependencies() {
            return List.of();
        }
    }


}
