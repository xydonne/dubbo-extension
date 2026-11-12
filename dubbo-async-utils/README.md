# dubbo-async-utils

Dubbo 异步调用工具类。以下示例针对本项目依赖的 `com.alibaba:dubbo:2.5.3`，源码级别为 Java 7。

## 安装与依赖

在仓库根目录执行：

```sh
mvn -f dubbo-async-utils/pom.xml install
```

在消费者项目中添加依赖。工具模块把 Dubbo 声明为 `optional`，因此消费者需要自己引入 Dubbo：

```xml
<dependency>
    <groupId>com.github.xydonne</groupId>
    <artifactId>dubbo-async-utils</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dubbo</artifactId>
    <version>2.5.3</version>
</dependency>
```

## 示例：异步调用 doSomeThing()

以下是接入已有 Dubbo 服务的消费者示例，需要先启动提供者，并使用与提供者相同的接口类；它不是独立启动服务的完整工程。

假设提供者已经暴露以下接口，成功调用时返回一个字符串：

```java
package com.example;

public interface Service {
    String doSomeThing();
}
```

### 1. 为消费者方法开启异步调用

将下面的 reference 配置放进消费者现有的 Spring XML 中。根元素需要声明 `dubbo` 命名空间和对应 schema；保留项目已有的 application、registry 等配置。

```xml
<dubbo:reference id="service" interface="com.example.Service">
    <dubbo:method name="doSomeThing" async="true" />
</dubbo:reference>
```

这里的 `service` 必须是这个 reference 创建的 Dubbo 代理，不能用本地实现对象代替。

### 2. 在调用后立即注册回调

将以下类放入消费者项目，并把上面的 `service` bean 传入 `invoke`：

```java
package com.example;

import com.github.xydonne.dubbo.async.utils.DubboAsyncUtils;
import com.github.xydonne.dubbo.async.utils.DubboCallback;
import com.github.xydonne.dubbo.async.utils.DubboFuture;

public final class AsyncConsumer {
    public static DubboFuture<String> invoke(Service service) {
        return DubboAsyncUtils.async(service.doSomeThing(),
                new DubboCallback<String>() {
                    @Override
                    public void apply(String response) {
                        System.out.println("回调结果：" + response);
                    }

                    @Override
                    public void caught(Throwable throwable) {
                        try {
                            // 处理超时、网络等底层异步调用异常。
                            throwable.printStackTrace();
                        } finally {
                            // 保留基类的完成标记和唤醒等待线程的行为。
                            super.caught(throwable);
                        }
                    }
                });
    }
}
```

`service.doSomeThing()` 会先发起异步 RPC；对于这个 String 返回值，代理立即返回的通常是 `null`，真正的结果通过回调或 Future 获取。`async` 的第一个参数用于推断结果类型；实现实际读取的是当前线程 `RpcContext` 中最近一次调用的 Future。

因此，发起 RPC 和调用 `DubboAsyncUtils.async(...)` 必须在同一个线程中紧接着完成，中间不要插入其他 Dubbo 调用。上面的写法将两步放在一个表达式里。

### 3. 需要结果时使用 Future

在能访问 `service` bean 的消费者代码中调用：

```java
DubboFuture<String> future = AsyncConsumer.invoke(service);
// 这里可以继续处理其他工作，无需立即阻塞等待。

// 只有需要结果时才等待；调用方需要处理下面的受检异常。
String result = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
```

`get` 可能抛出 `InterruptedException`、`ExecutionException` 和 `TimeoutException`。应用应按业务需要处理异常，并在捕获中断后恢复线程中断状态。

## 使用注意

- 示例需要消费者进程保持运行，直到回调处理完成；短命命令行程序应自己安排有界等待再退出。`future.get(...)` 返回不代表回调中的业务代码已经执行完毕。
- 回调可能由 Dubbo 的响应处理线程执行；不要在回调中做长时间阻塞工作，也不要在回调内部调用同一个 Future 的 `done()`。
- 当前 `DubboCallback.done(Object)` 直接读取 `Result.getValue()`，不会把服务端业务异常转交给 `caught`。因此不能仅凭 `apply` 被调用就判断业务成功；需要通过 `future.get(...)` 检查调用结果及业务异常。
- 当前 `done(timeout, unit)` 的回调等待会重复使用同一个超时值，不能保证总等待时间有界。需要限制等待时间时使用 `future.get(timeout, unit)`；它仅限制获取 RPC 结果的等待，不会取消请求，也不会等待回调业务完成。

此示例对应 [issue #1：可以提供一个 demo 吗？](https://github.com/xydonne/dubbo-extension/issues/1)。
