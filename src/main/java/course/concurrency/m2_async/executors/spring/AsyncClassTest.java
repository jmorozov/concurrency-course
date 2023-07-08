package course.concurrency.m2_async.executors.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/*
* 3. По умолчанию:
* runAsyncTask: task-1
* internalTask: task-1
* 
* 4. Если запустить один метод, помеченный @Async в другом методе с @Async, то они выполнятся на одном executor в одном и том же потоке.
* Поведение логично, потому что вызывается напрямую внутренний метод одного класса, а не метод прокси, сгенерированного Spring.
* 
* 5. Явно руками вызывать метод internalTask на executor - executor.submit(this::internalTask); внутри метода runAsyncTask
* 
* 6. 
* builder = {TaskExecutorBuilder@3951} 
 queueCapacity = {Integer@3954} 2147483647
 corePoolSize = {Integer@3955} 8
 maxPoolSize = {Integer@3956} 2147483647
 allowCoreThreadTimeOut = {Boolean@3957} true
 keepAlive = {Duration@3958} "PT1M"
 awaitTermination = {Boolean@3959} false
 awaitTerminationPeriod = null
 threadNamePrefix = "task-"
 taskDecorator = null
 customizers = {Collections$UnmodifiableSet@3961}  size = 0
* 
* */
@Component
public class AsyncClassTest {

    @Autowired
    public ApplicationContext context;

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private ThreadPoolTaskExecutor executor;

    @Async
    public void runAsyncTask() {
        System.out.println("runAsyncTask: " + Thread.currentThread().getName());
        executor.submit(this::internalTask);
    }

    @Async
    public void internalTask() {
        System.out.println("internalTask: " + Thread.currentThread().getName());
    }
}
