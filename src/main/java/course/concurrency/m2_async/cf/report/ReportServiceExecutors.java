package course.concurrency.m2_async.cf.report;

import course.concurrency.m2_async.cf.LoadGenerator;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ReportServiceExecutors {

    /*
    * 1. Напишите число ядер в вашем процессоре. Если у него есть hyper-threading, то отметьте этот факт.
    *   Chip:	Apple M1
    *   Total Number of Cores:	8 (4 performance and 4 efficiency)
    * 
    * 3. Поставьте в LoadGenerator метод sleep и поэкспериментируйте с экзекьютором в классе ReportServiceExecutors. 
    * Попробуйте разные виды и параметры, запишите время выполнения теста при разных вариантах
    * Например, если у вас 8 ядер, то для fixed экзекьютора можно попробовать значения 4, 8, 16, 24, 32.
    * Тогда будет хорошо видна связь между количеством ядер и результатом.
    * Не забывайте и про другие экзекьюторы!
    * 
    * newCachedThreadPool() - 15089
    * newFixedThreadPool(4) - 136913
    * newFixedThreadPool(8) - 69231
    * newFixedThreadPool(16) - 36128
    * newFixedThreadPool(24) - 24104
    * newFixedThreadPool(32) - 19583
    * newFixedThreadPool(64) - 15089
    * newFixedThreadPool(128) - 15099
    * newSingleThreadExecutor() - 300005
    * newWorkStealingPool() - 73790
    * 
    * 4. Поставьте в LoadGenerator метод compute и поэкспериментируйте с экзекьютором в классе ReportServiceExecutors. 
    * Запишите время выполнения теста при разных вариантах
    * 
    * newCachedThreadPool() - 3421
    * newFixedThreadPool(4) - 3757
    * newFixedThreadPool(8) - 3123
    * newFixedThreadPool(16) - 3714
    * newFixedThreadPool(24) - 3483
    * newFixedThreadPool(32) - 3387
    * newFixedThreadPool(64) - 3589
    * newFixedThreadPool(128) - 3274
    * newSingleThreadExecutor() - 12137
    * newWorkStealingPool() - 3162
    * 
    * 5. Проанализируйте полученные цифры и напишите свои выводы и впечатления:)
    * (приложите, пожалуйста, результаты из пунктов 2 и 3)
    * */

//	private ExecutorService executor = Executors.newCachedThreadPool();
//	private ExecutorService executor = Executors.newFixedThreadPool(4);
//	private ExecutorService executor = Executors.newFixedThreadPool(8);
//	private ExecutorService executor = Executors.newFixedThreadPool(16);
//	private ExecutorService executor = Executors.newFixedThreadPool(24);
//	private ExecutorService executor = Executors.newFixedThreadPool(32);
//	private ExecutorService executor = Executors.newFixedThreadPool(64);
//	private ExecutorService executor = Executors.newFixedThreadPool(128);
//	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private ExecutorService executor = Executors.newWorkStealingPool();

	private LoadGenerator loadGenerator = new LoadGenerator();

	public Others.Report getReport() {
		Future<Collection<Others.Item>> iFuture =
				executor.submit(() -> getItems());
		Future<Collection<Others.Customer>> customersFuture =
				executor.submit(() -> getActiveCustomers());

		try {
			Collection<Others.Customer> customers = customersFuture.get();
			Collection<Others.Item> items = iFuture.get();
			return combineResults(items, customers);
		} catch (ExecutionException | InterruptedException ex) {
		}

		return new Others.Report();
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
