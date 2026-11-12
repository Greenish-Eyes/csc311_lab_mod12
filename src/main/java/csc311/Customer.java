package csc311;

import java.util.concurrent.ThreadLocalRandom;

public class Customer implements Runnable {
    private final int id;
    private final CheckoutQueue queue;
    private final int shopMillis;

    public Customer(int id, CheckoutQueue queue, int shopMillis) {
        this.id = id;
        this.queue = queue;
        this.shopMillis = ThreadLocalRandom.current().nextInt(1, shopMillis + 1);
    }

}
