package com.lei.java.forge.jmh.async;

import com.lei.java.forge.jmh.AbstractBenchmark;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 异步benchmark测试
 * 1. RxJava
 * 2. Reactor
 * 3. CompletableFuture
 * </p>
 *
 * @author 伍磊
 */
@BenchmarkMode(value = Mode.Throughput)
@OutputTimeUnit(value = TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 2)
@State(Scope.Thread)
public class AsyncBenchmark extends AbstractBenchmark {

    @Param({"1", "10", "100"})
    private int sleepTimeMs; // 测试不同的工作量

    @Param({"1", "4"})
    private int threadCount; // 测试不同的线程池大小


    private ExecutorService executorService;

    @Setup
    public void set() {
        // 创建共享线程池以确保公平测试
        executorService = Executors.newFixedThreadPool(
                threadCount
        );
    }

    @TearDown
    public void clear() {
        executorService.shutdown();
        executorService.shutdownNow();
    }

    @Benchmark
    public Disposable testRxjava() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Disposable subscribe = Observable.fromSupplier(() -> 1)
                .subscribeOn(Schedulers.from(executorService))
                .subscribe(t -> {
                    Thread.sleep(Duration.ofMillis(sleepTimeMs));
                    countDownLatch.countDown();
                });
        countDownLatch.await();
        return subscribe;
    }

    @Benchmark
    public reactor.core.Disposable testReactor() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        reactor.core.Disposable subscribe = Mono.just(1)
                .subscribeOn(reactor.core.scheduler.Schedulers.fromExecutor(executorService))
                .subscribe(t -> {
                    try {
                        Thread.sleep(Duration.ofMillis(sleepTimeMs));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    countDownLatch.countDown();
                });
        countDownLatch.await();
        return subscribe;
    }

    @Benchmark
    public Integer testCompletableFuture() throws InterruptedException, ExecutionException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(Duration.ofMillis(sleepTimeMs));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            countDownLatch.countDown();
        }, executorService);
        countDownLatch.await();
        return 1;
    }


    public static void main(String[] args) throws InterruptedException, RunnerException {
        runBenchmark(AsyncBenchmark.class);
    }
}
