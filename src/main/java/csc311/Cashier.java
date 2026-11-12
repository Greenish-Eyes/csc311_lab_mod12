package csc311;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class Cashier implements Runnable {
    public static final int CLOSE = -1;

    private final int id;
    private final CheckoutQueue queue;
    private final int minServiceMillis, maxServiceMillis;
    private final Stats stats;
    private long busyMillis = 0;

    public Cashier(int id, CheckoutQueue queue, int minServiceMillis, int maxServiceMillis, Stats stats) {
        this.id = id;
        this.queue = queue;
        this.minServiceMillis = minServiceMillis;
        this.maxServiceMillis = maxServiceMillis;
        this.stats = stats;
    }

    public static class Stats{
        public final AtomicLong totalWait = new AtomicLong();
        public final AtomicLong maxWait = new AtomicLong();
        public final AtomicLong served = new AtomicLong();
    }


    public long getBusyMillis(){
        return busyMillis;
    }

    @Override
    public void run() {
        // Implementation for the cashier's task
        try {
            while (true) {
                int customerId = queue.next();
                if (customerId == CLOSE) {
                    break; // Exit the loop if the cashier is closed
                }

                long wait = queue.waitMillis(customerId);
                stats.totalWait.addAndGet(wait);
                stats.maxWait.accumulateAndGet(wait, Math::max);

                // Simulate service time
                int service = ThreadLocalRandom.current().nextInt(minServiceMillis, maxServiceMillis + 1);
                Thread.sleep(service);
                busyMillis += service;
                stats.served.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
