package kuan.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * @author qinxuekuan
 * @date 2022/6/15
 */
public interface Context {
    /**
     * @param type 从容器中需要获取组件的类型
     * @return 一个组件，可以 null。 用 Optional 装载
     */
    default Optional get(Type type) {
        return get(Ref.of(type));
    }

    // 期望得到这样的一个方法： Optional get(Ref type)，  Ref 是对 Class 和 ParameterizedType 的封装
    Optional get(Ref ref);

    class Ref {
        private Type container;
        private Class<?> component;

        Ref(ParameterizedType container) {
            this.container = container.getRawType();
            this.component = (Class<?>) container.getActualTypeArguments()[0];
        }

        Ref(Class<?> component) {
            this.component = component;
        }

        static public Ref of(Type type) {
            if (type instanceof ParameterizedType) {
                return new Ref((ParameterizedType) type);
            }
            return new Ref((Class<?>) type);
        }

        public Type getContainer() {
            return container;
        }

        public Class<?> getComponent() {
            return component;
        }

        public boolean isContainer() {
            return this.container != null;
        }
    }

}
