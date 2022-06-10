package kuan.tdd.di.exception;

/**
 * @author qinxuekuan
 * @date 2022/6/8
 */
public class DependencyNotFoundException extends RuntimeException {

    private final Class<?> dependency;

    public DependencyNotFoundException(Class<?> dependency) {
        this.dependency = dependency;
    }

    public Class<?> getDependency() {
        return dependency;
    }


}
