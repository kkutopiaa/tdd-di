package kuan.tdd.di;

import java.util.List;

interface ComponentProvider<T> {
    // ConstructorInjectionProvider 需要有 context 这个上下文（并不是每次去 getContext 的时候都是一个新的 Context），但 ContextConfig 又提供不了。
    // 用这个接口提供 Context。 代表在传入的 Context 上下文中，获取 T 对象。
    T get(Context context);


    // 期望得到这样的一个方法： List<Ref> getDependencies()，  Ref 是对 Class 和 ParameterizedType 的封装
    default List<ComponentRef<?>> getDependencies() {
        return List.of();
    }
}
