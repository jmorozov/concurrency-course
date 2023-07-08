package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public class PriceAggregator {

	private PriceRetriever priceRetriever = new PriceRetriever();

	public void setPriceRetriever(PriceRetriever priceRetriever) {
		this.priceRetriever = priceRetriever;
	}

	private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

	public void setShops(Collection<Long> shopIds) {
		this.shopIds = shopIds;
	}

	public double getMinPrice(long itemId) {
		ExecutorService executor = Executors.newFixedThreadPool(128);
		// place for your code
		var result = shopIds.stream()
				.map(shopId ->
						CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor)
								.orTimeout(2800L, TimeUnit.MILLISECONDS)
								.exceptionally((exception) -> {
									System.out.println("Exception happen: " + exception);
									return null;
								})
				).collect(
						collectingAndThen(
								toList(),
								l -> CompletableFuture.allOf(l.toArray(new CompletableFuture[0]))
										.thenApply(__ -> l.stream()
												.map(CompletableFuture::join)
												.collect(toList()))
						)
				);

		try {
			return result.get().stream().filter(Objects::nonNull).min(Double::compareTo).orElse(Double.NaN);
		} catch (InterruptedException | ExecutionException e) {
			return Double.NaN;
		}
	}
}
