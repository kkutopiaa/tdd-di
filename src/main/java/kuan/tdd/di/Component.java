package kuan.tdd.di;

import java.lang.annotation.Annotation;

/**
 * @author qinxuekuan
 * @date 2022/7/9
 */
public record Component(Class<?> type, Annotation qualifier) {
}
