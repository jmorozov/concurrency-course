package course.concurrency.exams.refactoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.*;

public class MountTableRefresherServiceTests {

	private MountTableRefresherService service;

	private Others.RouterStore routerStore;
	private Others.MountTableManager manager;
	private Others.LoadingCache routerClientsCache;

	private final List<String> TEST_ADDRESSES = List.of("123", "local6", "789", "local");

	@BeforeEach
	public void setUpStreams() {
		service = new MountTableRefresherService();
		service.setCacheUpdateTimeout(1000);
		routerStore = mock(Others.RouterStore.class);
		manager = mock(Others.MountTableManager.class);
		service.setRouterStore(routerStore);
		routerClientsCache = mock(Others.LoadingCache.class);
		service.setRouterClientsCache(routerClientsCache);
		// service.serviceInit(); // needed for complex class testing, not for now

		List<Others.RouterState> states = TEST_ADDRESSES.stream()
				.map(Others.RouterState::new)
				.collect(toList());
		when(routerStore.getCachedRecords()).thenReturn(states);
	}

	@AfterEach
	public void restoreStreams() {
		// service.serviceStop();
	}

	@Test
	@DisplayName("All tasks are completed successfully")
	public void allDone() {
		// given
		MountTableRefresherService mockedService = Mockito.spy(service);

		List<MountTableRefreshTask> tasks = TEST_ADDRESSES.stream()
				.map(addr -> new MountTableRefreshTask(manager, addr))
				.collect(toList());
		AtomicInteger taskIndex = new AtomicInteger(0);
		when(mockedService.getRefreshTasks(anyString())).thenAnswer(inv -> tasks.get(taskIndex.getAndIncrement()));

		when(manager.refresh()).thenReturn(true);

		// when
		mockedService.refresh();

		// then
		verify(mockedService).log("Mount table entries cache refresh successCount=4,failureCount=0");
		verify(routerClientsCache, never()).invalidate(anyString());
	}

	@Test
	@DisplayName("All tasks failed")
	public void noSuccessfulTasks() {
		// given
		MountTableRefresherService mockedService = Mockito.spy(service);

		List<MountTableRefreshTask> tasks = TEST_ADDRESSES.stream()
				.map(address -> new MountTableRefreshTask(manager, address))
				.collect(toList());
		AtomicInteger taskIndex = new AtomicInteger(0);
		when(mockedService.getRefreshTasks(anyString())).thenAnswer(inv -> tasks.get(taskIndex.getAndIncrement()));

		when(manager.refresh()).thenReturn(false);

		// when
		mockedService.refresh();

		// then
		verify(mockedService).log("Not all router admins updated their cache");
		verify(mockedService).log("Mount table entries cache refresh successCount=0,failureCount=4");
		TEST_ADDRESSES.forEach(addr -> verify(routerClientsCache).invalidate(addr));
	}

	@Test
	@DisplayName("Some tasks failed")
	public void halfSuccessedTasks() {
		// given
		MountTableRefresherService mockedService = Mockito.spy(service);

		Others.MountTableManager managerSucceed = mock(Others.MountTableManager.class);
		Others.MountTableManager managerFailed = mock(Others.MountTableManager.class);
		when(managerFailed.refresh()).thenReturn(false);
		when(managerSucceed.refresh()).thenReturn(true);

		List<MountTableRefreshTask> tasks = new ArrayList<>();
		tasks.add(new MountTableRefreshTask(managerSucceed, TEST_ADDRESSES.get(0)));
		tasks.add(new MountTableRefreshTask(managerFailed, TEST_ADDRESSES.get(1)));
		tasks.add(new MountTableRefreshTask(managerSucceed, TEST_ADDRESSES.get(2)));
		tasks.add(new MountTableRefreshTask(managerFailed, TEST_ADDRESSES.get(3)));

		AtomicInteger taskIndex = new AtomicInteger(0);
		when(mockedService.getRefreshTasks(anyString())).thenAnswer(inv -> tasks.get(taskIndex.getAndIncrement()));

		// when
		mockedService.refresh();

		// then
		verify(mockedService).log("Not all router admins updated their cache");
		verify(mockedService).log("Mount table entries cache refresh successCount=2,failureCount=2");
		verify(routerClientsCache).invalidate(TEST_ADDRESSES.get(1));
		verify(routerClientsCache).invalidate(TEST_ADDRESSES.get(3));
	}

	@Test
	@DisplayName("One task completed with exception")
	public void exceptionInOneTask() {
		// given
		MountTableRefresherService mockedService = Mockito.spy(service);

		Others.MountTableManager managerSucceed = mock(Others.MountTableManager.class);
		Others.MountTableManager brokenManager = mock(Others.MountTableManager.class);
		when(brokenManager.refresh()).thenThrow(new RuntimeException());
		when(managerSucceed.refresh()).thenReturn(true);

		List<MountTableRefreshTask> tasks = new ArrayList<>();
		tasks.add(new MountTableRefreshTask(managerSucceed, TEST_ADDRESSES.get(0)));
		tasks.add(new MountTableRefreshTask(brokenManager, TEST_ADDRESSES.get(1)));
		tasks.add(new MountTableRefreshTask(managerSucceed, TEST_ADDRESSES.get(2)));
		tasks.add(new MountTableRefreshTask(managerSucceed, TEST_ADDRESSES.get(3)));

		AtomicInteger taskIndex = new AtomicInteger(0);
		when(mockedService.getRefreshTasks(anyString())).thenAnswer(inv -> tasks.get(taskIndex.getAndIncrement()));

		// when
		mockedService.refresh();

		// then
		verify(mockedService).log("java.util.concurrent.CompletionException: java.lang.RuntimeException");
		verify(mockedService).log("Not all router admins updated their cache");
		verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
		verify(routerClientsCache).invalidate(TEST_ADDRESSES.get(1));
	}

	@Test
	@DisplayName("One task exceeds timeout")
	public void oneTaskExceedTimeout() {
		MountTableRefresherService mockedService = Mockito.spy(service);
		
		Others.MountTableManager managerSucceed = mock(Others.MountTableManager.class);
		Others.MountTableManager managerTimeout = mock(Others.MountTableManager.class);
		when(managerTimeout.refresh()).thenAnswer(inv -> {
			Thread.sleep(3000);
			return true;
		});
		when(managerSucceed.refresh()).thenReturn(true);

		List<MountTableRefreshTask> tasks = new ArrayList<>();
		tasks.add(new MountTableRefreshTask(managerSucceed, TEST_ADDRESSES.get(0)));
		tasks.add(new MountTableRefreshTask(managerTimeout, TEST_ADDRESSES.get(1)));
		tasks.add(new MountTableRefreshTask(managerSucceed, TEST_ADDRESSES.get(2)));
		tasks.add(new MountTableRefreshTask(managerSucceed, TEST_ADDRESSES.get(3)));

		AtomicInteger taskIndex = new AtomicInteger(0);
		when(mockedService.getRefreshTasks(anyString())).thenAnswer(inv -> tasks.get(taskIndex.getAndIncrement()));

		mockedService.refresh();

		verify(mockedService).log("Not all router admins updated their cache");
		verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
		verify(routerClientsCache).invalidate(TEST_ADDRESSES.get(1));
	}

}
