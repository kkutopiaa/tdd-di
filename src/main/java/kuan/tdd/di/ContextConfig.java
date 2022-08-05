package kuan.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import kuan.tdd.di.exception.CyclicDependenciesFoundException;
import kuan.tdd.di.exception.DependencyNotFoundException;
import kuan.tdd.di.exception.IllegalComponentException;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author qinxuekuan
 * @date 2022/6/6
 */
public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private Map<Class<?>, ScopeProvider> scopes = new HashMap<>();

    public ContextConfig() {
        scope(Singleton.class, SingletonProvider::new);
    }

    public <T> void bind(Class<T> type, T instance) {
        components.put(new Component(type, null), context -> instance);
    }

    public <T> void bind(Class<T> type, T instance, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), context -> instance);
        }
    }


    public <T, Implementation extends T> void bind(Class<T> type, Class<Implementation> implementation) {
        bind(type, implementation, implementation.getAnnotations());
    }

    public <T, Implementation extends T>
    void bind(Class<T> type, Class<Implementation> implementation, Annotation... annotations) {
        // 将 annotation 分成 3 个组： 带Scope的，带Qualifier的，其他异常情况的（Illegal）
        Map<Class<?>, List<Annotation>> annotationGroups = Arrays.stream(annotations)
                .collect(Collectors.groupingBy(this::typeOf, Collectors.toList()));

        if (annotationGroups.containsKey(Illegal.class)) {
            throw new IllegalComponentException();
        }

        bind(type, annotationGroups.getOrDefault(Qualifier.class, List.of()),
                createScopeProvider(implementation, annotationGroups.getOrDefault(Scope.class, List.of())));
    }

    private <T, Implementation extends T> ComponentProvider<?>
    createScopeProvider(Class<Implementation> implementation, List<Annotation> scopes) {
        if (scopes.size() > 1) {
            throw new IllegalComponentException();
        }
        ComponentProvider<Implementation> injectionProvider = new InjectionProvider<>(implementation);
        Optional<Annotation> scope = scopes.stream().findFirst()
                .or(() -> scopeFrom(implementation));
        return scope.<ComponentProvider<?>>map(s -> getScopeProvider(s, injectionProvider))
                .orElse(injectionProvider);
    }

    private <T> void bind(Class<T> type, List<Annotation> qualifiers, ComponentProvider<?> provider) {
        if (qualifiers.isEmpty()) {
            components.put(new Component(type, null), provider);
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), provider);
        }
    }

    private <T> Optional<Annotation> scopeFrom(Class<T> implementation) {
        return Arrays.stream(implementation.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst();
    }

    private Class<?> typeOf(Annotation annotation) {
        Class<? extends Annotation> type = annotation.annotationType();
        return Stream.of(Qualifier.class, Scope.class)
                .filter(type::isAnnotationPresent).findFirst()
                .orElse(Illegal.class);
    }

    private @interface Illegal {

    }

    private ComponentProvider<?> getScopeProvider(Annotation scope, ComponentProvider<?> provider) {
        if (!scopes.containsKey(scope.annotationType())) {
            throw new IllegalComponentException();
        }
        return scopes.get(scope.annotationType())
                .create(provider);
    }

    public <ScopeType extends Annotation> void scope(Class<ScopeType> scope,
                                                     ScopeProvider provider) {
        scopes.put(scope, provider);
    }

    public Context getContext() {
        // 检查是否存在依赖
        // 检查是否发生了循环依赖
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {

            @Override
            public <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref) {
                if (ref.getQualifier() != null) {
                    return Optional.ofNullable(components.get(new Component(ref.getComponent(), ref.getQualifier())))
                            .map(provider -> (ComponentType) provider.get(this));
                }

                if (ref.isContainer()) {
                    if (ref.container() != Provider.class) {
                        return Optional.empty();
                    }
                    return (Optional<ComponentType>) Optional.ofNullable(components.get(new Component(ref.getComponent(), ref.getQualifier())))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(components.get(new Component(ref.getComponent(), ref.getQualifier())))
                        .map(provider -> (ComponentType) provider.get(this));
            }
        };
    }

    public void checkDependencies(Component component, Stack<Component> visiting) {
        for (ComponentRef<?> dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(new Component(dependency.getComponent(), dependency.getQualifier()))) {
                throw new DependencyNotFoundException(component, dependency.component());
            }
            if (!dependency.isContainer()) {
                if (!components.containsKey(new Component(dependency.getComponent(), dependency.getQualifier()))) {
                    throw new DependencyNotFoundException(component, dependency.component());
                }
                if (visiting.contains(dependency.component())) {
                    throw new CyclicDependenciesFoundException(visiting);
                }
                visiting.push(dependency.component());
                checkDependencies(new Component(dependency.getComponent(), dependency.getQualifier()), visiting);
                visiting.pop();
            }
        }
    }


}
