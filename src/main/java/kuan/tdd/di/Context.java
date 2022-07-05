package kuan.tdd.di;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * @author qinxuekuan
 * @date 2022/6/15
 */
public interface Context {
    /**
     * @param type   从容器中需要获取组件的类型
     * @return 一个组件，可以 null。 用 Optional 装载
     */
    Optional getType(Type type);
}
