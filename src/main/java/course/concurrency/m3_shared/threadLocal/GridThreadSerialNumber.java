package course.concurrency.m3_shared.threadLocal;

import java.util.concurrent.atomic.AtomicInteger;

class GridThreadSerialNumber {
	private final AtomicInteger nextSerialNum = new AtomicInteger(0);

	private final ThreadLocal<Integer> serialNum = ThreadLocal.withInitial(nextSerialNum::getAndIncrement);

	public int get() {
		return serialNum.get();
	}
}
