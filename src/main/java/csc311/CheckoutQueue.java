package csc311;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class CheckoutQueue {
    private final BlockingQueue<Integer> queue;

    private final ConcurrentHashMap<Integer, Long> arrivalTimes = new ConcurrentHashMap<>();

    public CheckoutQueue(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    public void join(int customerId) throws InterruptedException {
        arrivalTimes.put(customerId, System.nanoTime());;
        queue.put(customerId);
    }

    public int next() throws InterruptedException {
        return queue.take();
    }
}
