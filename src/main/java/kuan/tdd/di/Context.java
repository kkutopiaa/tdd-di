package kuan.tdd.di;

import java.util.Optional;

/**
 * @author qinxuekuan
 * @date 2022/6/15
 */
public interface Context {
    <Type> Optional<Type> get(Class<Type> type);
}
