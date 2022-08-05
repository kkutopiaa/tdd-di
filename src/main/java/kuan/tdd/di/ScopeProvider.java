package kuan.tdd.di;

interface ScopeProvider {
    ComponentProvider<?> create(ComponentProvider<?> provider);
}
