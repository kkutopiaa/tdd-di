# tdd-di-container



## 一： 简介
用 TDD 的方式，实现一个依赖注入容器，以 Jakarta EE 中的 Jakarta Dependency Injection 为参考，并对其适当简化。  
关于依赖注入的来龙去脉，参考 Martin Fowler 在 2004 年写的文章：[IoC 容器与依赖注入模式](https://martinfowler.com/articles/injection.html)。

Jakarta Dependency Injection 主要功能包含：
1. 组件的构造  
2. 依赖的选择  
3. 生命周期控制
   
 
Jakarta Dependency Injection 中没有规定而又常用的部分有：
1. 配置容器如何配置  
2. 容器层级结构  
3. 生命周期回调

## 二： 任务列表
> 已是最终版本，包含了在开发过程中补充及优化进去的各种任务


### 1. 组件的构造
- 无需构造的组件——组件实例
- 如果注册的组件不可实例化，则抛出异常
    - 抽象类
    - 接口
- 构造函数注入
    - 无依赖的组件应该通过默认构造函数生成组件实例
    - 有依赖的组件，通过 Inject 标注的构造函数生成组件实例
    - 如果所依赖的组件也存在依赖，那么需要对所依赖的组件也完成依赖注入
    - 如果组件有多于一个 Inject 标注的构造函数，则抛出异常
    - 如果组件没有 Inject 标注的构造函数，也没有默认构造函数
    - 如果组件需要的依赖不存在，则抛出异常
    - 如果组件间存在循环依赖，则抛出异常
- 字段注入
    - 通过 Inject 标注将字段声明为依赖组件
    - 如果字段为 final 则抛出异常
    - 依赖中应包含 Inject Field 声明的依赖
- 方法注入
    - 通过 Inject 标注的方法，其参数为依赖组件
    - 通过 Inject 标注的无参数方法，会被调用
    - 按照子类中的规则，覆盖父类中的 Inject 方法
    - 如果方法定义了类型参数，则抛出异常
    - 依赖中应包含 Inject Method 声明的依赖

### 2. 依赖的选择
- 对 Provider 类型的依赖
    - 从容器中取得组件的 Provider
    - 注入构造函数中可以声明对于 Provider 的依赖
    - 注入	字段中可以声明对于 Provider 的依赖
    - 注入	方法中可声明对于 Provider 的依赖
    - 将构造函数中的 Provider 加入依赖
    - 将字段中的 Provider 加入依赖
    - 将方法中的 Provider 加入依赖

- 自定义 Qualifier 的依赖
    - 注册组件时，可额外指定 Qualifier
        - 针对 instance 指定一个 Qualifier
        - 针对组件指定一个 Qualiifer
        - 针对 instance 指定多个 Qualifieri
        - 针对组件指定多个 Qualiifer
    - 注册组件时，如果不是合法的 Qualifier，则不接受组件注册
    - 寻找依赖时，需同时满足类型与自定义 Qualifier 标注
        - 在检查依赖时使用 Qualifier
        - 在检查循环依赖时使用 Qualifier
        - 构造函数注入可以使用 Qualifier 声明依赖
            - 依赖中包含 Qualifier
            - 如果不是合法的 Qualifier，则组件非法
        - 字段注入可以使用 Qualifier 声明依赖
            - 依赖中包含 Qualifier
            - 如果不是合法的 Qualifier，则组件非法
        - 函数注入可以使用 Qualifier 声明依赖
            - 依赖中包含 Qualifier
            - 如果不是合法的 Qualifier，则组件非法

### 3. 声明周期控制
- Singleton 生命周期
    - 注册组件时，可额外指定是否为 Singleton
    - 注册包含 Qualifier 的组件时，可额外指定是否为 Singleton
    - 注册组件时，可从类对象上提取 Scope 标注
    - 容器组件默认不是 Single 生命周期
    - 包含 Qualifier 的组件默认不是 Single 生命周期
    - 对于包含 Scope 的组件，检测依赖关系
- 自定义 Scope 标注
    - 可向容器注册自定义 Scope 标注的回调