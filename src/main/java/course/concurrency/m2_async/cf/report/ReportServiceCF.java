package course.concurrency.m2_async.cf.report;

import course.concurrency.m2_async.cf.LoadGenerator;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

/*
* Напишите число ядер вашего компьютера
*   Chip:	Apple M1
*   Total Number of Cores:	8 (4 performance and 4 efficiency)
* 4. Поставьте в LoadGenerator метод sleep и поэкспериментируйте с экзекьютором в классе ReportServiceCF.
* Запишите время выполнения теста при разных вариантах
* newCachedThreadPool() - 15145
* newFixedThreadPool(4) - 136909
* newFixedThreadPool(8) - 69230
* newFixedThreadPool(16) - 36150
* newFixedThreadPool(24) - 24113
* newFixedThreadPool(32) - 19597
* newFixedThreadPool(64) - 15128
* newFixedThreadPool(128) - 15111
* newSingleThreadExecutor() - 300001
* newWorkStealingPool() - 69227
* ForkJoinPool.commonPool() - 79738
* 
* 5. Поставьте в LoadGenerator метод compute и поэкспериментируйте с экзекьютором в классе ReportServiceCF.
* Запишите время выполнения теста при разных вариантах
* newCachedThreadPool() - 3566
* newFixedThreadPool(4) - 3725
* newFixedThreadPool(8) - 3135
* newFixedThreadPool(16) - 3425
* newFixedThreadPool(24) - 3343
* newFixedThreadPool(32) - 3224
* newFixedThreadPool(64) - 3935
* newFixedThreadPool(128) - 3236
* newSingleThreadExecutor() - 12140
* newWorkStealingPool() - 3182
* ForkJoinPool.commonPool() - 3362
* 6. Проанализируйте цифры, которые у вас получились и напишите свои выводы и впечатления:)
* 
* */
public class ReportServiceCF {

//	private ExecutorService executor = Executors.newCachedThreadPool();
//	private ExecutorService executor = Executors.newFixedThreadPool(4);
//	private ExecutorService executor = Executors.newFixedThreadPool(8);
//	private ExecutorService executor = Executors.newFixedThreadPool(16);
//	private ExecutorService executor = Executors.newFixedThreadPool(24);
//	private ExecutorService executor = Executors.newFixedThreadPool(32);
//	private ExecutorService executor = Executors.newFixedThreadPool(64);
//	private ExecutorService executor = Executors.newFixedThreadPool(128);
//	private ExecutorService executor = Executors.newSingleThreadExecutor();
//    private ExecutorService executor = Executors.newWorkStealingPool();
    private ExecutorService executor = ForkJoinPool.commonPool();

    private LoadGenerator loadGenerator = new LoadGenerator();

    public Others.Report getReport() {
        CompletableFuture<Collection<Others.Item>> itemsCF =
                CompletableFuture.supplyAsync(() -> getItems(), executor);

        CompletableFuture<Collection<Others.Customer>> customersCF =
                CompletableFuture.supplyAsync(() -> getActiveCustomers(), executor);

        CompletableFuture<Others.Report> reportTask =
                customersCF.thenCombine(itemsCF,
                        (customers, orders) -> combineResults(orders, customers));

        return reportTask.join();
    }

    private Others.Report combineResults(Collection<Others.Item> items, Collection<Others.Customer> customers) {
        return new Others.Report();
    }

    private Collection<Others.Customer> getActiveCustomers() {
        loadGenerator.work();
        loadGenerator.work();
        return List.of(new Others.Customer(), new Others.Customer());
    }

    private Collection<Others.Item> getItems() {
        loadGenerator.work();
        return List.of(new Others.Item(), new Others.Item());
    }

    public void shutdown() {
        executor.shutdown();
    }
}
