package factory;

import java.util.ArrayDeque;
import java.util.List;

final class ConveyorBelt {
    private final int capacity;
    private final ArrayDeque<Part> parts = new ArrayDeque<>();
    private boolean productionComplete;

    ConveyorBelt(int capacity) {
        this.capacity = capacity;
    }

    synchronized void put(Part part) throws InterruptedException {
        while (parts.size() == capacity) {
            wait();
        }
        parts.addLast(part.enteringBelt());
        notifyAll();
    }

    synchronized Part take(long travelTimeNanos) throws InterruptedException {
        while (true) {
            while (parts.isEmpty() && !productionComplete) {
                wait();
            }
            if (parts.isEmpty()) {
                return null;
            }

            long remaining = travelTimeNanos - (System.nanoTime() - parts.peekFirst().beltEntryNanos());
            if (remaining <= 0) {
                Part part = parts.pollFirst();
                notifyAll();
                return part;
            }
            long millis = remaining / 1_000_000;
            int nanos = (int) (remaining % 1_000_000);
            wait(millis, nanos);
        }
    }

    synchronized void completeProduction() {
        productionComplete = true;
        notifyAll();
    }

    synchronized int size() {
        return parts.size();
    }

    int capacity() {
        return capacity;
    }

    synchronized List<Part> snapshot() {
        return List.copyOf(parts);
    }
}
