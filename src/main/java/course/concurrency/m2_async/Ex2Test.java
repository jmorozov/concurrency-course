package course.concurrency.m2_async;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Ex2Test {

	public static long longTask() throws InterruptedException {
		Thread.sleep(1000); // + try-catch
		return ThreadLocalRandom.current().nextInt();
	}

	public static void main(String[] args) {

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
		
		for (int i = 0; i < 10; i++) {
			executor.submit(Ex2Test::longTask);
			System.out.print(executor.getPoolSize() + " ");
		}

		executor.shutdown();
	}
}
