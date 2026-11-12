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
}
