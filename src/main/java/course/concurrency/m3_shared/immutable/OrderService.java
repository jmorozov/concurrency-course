package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {

	private final Map<Long, Order> currentOrders = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(0L);

	private long nextId() {
		return nextId.getAndIncrement();
	}

	public long createOrder(List<Item> items) {
		long id = nextId();

		Order order = Order.newWithItems(items)
				.id(id)
				.build();
		currentOrders.put(id, order);
		return id;
	}

	public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
		currentOrders.compute(
				orderId,
				(key, currentOrder) -> currentOrder == null ? null : currentOrder.withPaymentInfo(paymentInfo)
		);
		if (currentOrders.get(orderId).checkStatus()) {
			deliver(currentOrders.get(orderId));
		}
	}

	public void setPacked(long orderId) {
		currentOrders.compute(
				orderId,
				(key, currentOrder) -> currentOrder == null ? null : currentOrder.withPacked(true)
		);
		if (currentOrders.get(orderId).checkStatus()) {
			deliver(currentOrders.get(orderId));
		}
	}

	private void deliver(Order order) {
		/* ... */
		currentOrders.compute(
				order.getId(),
				(key, currentOrder) -> currentOrder == null ? null : currentOrder.withStatus(Order.Status.DELIVERED)
		);
	}

	public boolean isDelivered(long orderId) {
		Order order = currentOrders.get(orderId);
		return order != null && Order.Status.DELIVERED.equals(order.getStatus());
	}
}
