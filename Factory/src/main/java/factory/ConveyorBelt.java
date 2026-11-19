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

    synchronized Part take() throws InterruptedException {
        while (parts.isEmpty() && !productionComplete) {
            wait();
        }
        Part part = parts.pollFirst();
        notifyAll();
        return part;
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
