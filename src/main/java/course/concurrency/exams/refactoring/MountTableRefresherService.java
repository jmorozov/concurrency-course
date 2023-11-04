package course.concurrency.exams.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class MountTableRefresherService {

    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public void serviceInit()  {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<String, Others.RouterClient>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread();
                t.setName("MountTableRefresh_ClientsCacheCleaner");
                t.setDaemon(true);
                return t;
            }
        };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh()  {
        List<Others.RouterState> cachedRecords = routerStore.getCachedRecords();
        List<MountTableRefreshTask> refreshTasks = new ArrayList<>();
        for (Others.RouterState routerState : cachedRecords) {
            String adminAddress = routerState.getAdminAddress();
            if (adminAddress == null || adminAddress.length() == 0) {
                // this router has not enabled router admin.
                continue;
            }
            refreshTasks.add(getRefreshTasks(adminAddress));
        }
        if (!refreshTasks.isEmpty()) {
            invokeRefresh(refreshTasks);
        }
    }

    protected MountTableRefreshTask getRefreshTasks(String address) {
        if (isLocalAdmin(address)) {
            /*
             * Local router's cache update does not require RPC call, so no need for
             * RouterClient
             */
            return getLocalRefresher(address);
        } else {
            return  new MountTableRefreshTask(new Others.MountTableManager(address), address);
        }
    }

    protected MountTableRefreshTask getLocalRefresher(String adminAddress) {
        return new MountTableRefreshTask(new Others.MountTableManager("local"), adminAddress);
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private void invokeRefresh(List<MountTableRefreshTask> refreshTasks) {
        List<CompletableFuture<Void>> futureList = refreshTasks.stream()
                .map(task ->
                        CompletableFuture.runAsync(task::refresh)
                                .completeOnTimeout(null, cacheUpdateTimeout, TimeUnit.MILLISECONDS)
                                .exceptionally(ex -> {
                                    log(ex.toString());
                                    return null;
                                })
                )
                .collect(Collectors.toList());
        CompletableFuture.allOf(futureList.toArray(CompletableFuture[]::new)).join();
        logResult(refreshTasks);
    }

    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    private void logResult(List<MountTableRefreshTask> refreshTasks) {
        int successCount = 0;
        int failureCount = 0;
        for (MountTableRefreshTask mountTableRefreshThread : refreshTasks) {
            if (mountTableRefreshThread.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
                // remove RouterClient from cache so that new client is created
                removeFromCache(mountTableRefreshThread.getAdminAddress());
            }
        }
        if (failureCount != 0) {
            log("Not all router admins updated their cache");
        }
        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                successCount, failureCount));
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }
    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}