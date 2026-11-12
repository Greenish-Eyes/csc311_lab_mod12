package csc311;

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


    @Override
    public void run() {
        // Implementation for the cashier's task
    }
}
