package kuan.tdd.di;

import java.util.Optional;

/**
 * @author qinxuekuan
 * @date 2022/6/15
 */
public interface Context {

    // 期望得到这样的一个方法： Optional get(Ref type)，  Ref 是对 Class 和 ParameterizedType 的封装
    <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref);

}
