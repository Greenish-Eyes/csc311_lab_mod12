package factory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class Factory {
    public enum State {
        WORKING, BLOCKED, IDLE, DONE
    }

    private final int assemblerCount;
    private final int packagerCount;
    private final int totalParts;
    private final double speed;
    private final ConveyorBelt belt;
    private final State[] assemblerStates;
    private final State[] packagerStates;
    private final Thread[] workers;
    private final AtomicInteger built = new AtomicInteger();
    private final AtomicInteger packaged = new AtomicInteger();
    private final AtomicInteger beltSamples = new AtomicInteger();
    private final AtomicLong totalBeltNanos = new AtomicLong();

    private volatile boolean running;
    private volatile boolean stopped;
    private volatile long startNanos;
    private volatile long finishNanos;
    private int completedAssemblers;
    private int completedPackagers;

    public Factory(int assemblerCount, int packagerCount, int beltCapacity,
                   int totalParts, double speed) {
        this.assemblerCount = assemblerCount;
        this.packagerCount = packagerCount;
        this.totalParts = totalParts;
        this.speed = speed;
        belt = new ConveyorBelt(beltCapacity);
        assemblerStates = new State[assemblerCount];
        packagerStates = new State[packagerCount];
        workers = new Thread[assemblerCount + packagerCount];
        java.util.Arrays.fill(assemblerStates, State.IDLE);
        java.util.Arrays.fill(packagerStates, State.IDLE);
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        startNanos = System.nanoTime();
        int workerIndex = 0;
        for (int i = 0; i < packagerCount; i++) {
            int packager = i;
            workers[workerIndex++] = new Thread(() -> packageParts(packager),
                    "factory-packager-" + (i + 1));
        }
        for (int i = 0; i < assemblerCount; i++) {
            int assembler = i;
            workers[workerIndex++] = new Thread(() -> assembleParts(assembler),
                    "factory-assembler-" + (i + 1));
        }
        for (Thread worker : workers) {
            worker.start();
        }
    }

    public void stop() {
        stopped = true;
        running = false;
        finishNanos = System.nanoTime();
        for (Thread worker : workers) {
            if (worker != null) {
                worker.interrupt();
            }
        }
        belt.completeProduction();
    }

    private void assembleParts(int assembler) {
        try {
            for (int id = assembler + 1; id <= totalParts; id += assemblerCount) {
                setAssemblerState(assembler, State.WORKING);
                Thread.sleep(workDelayMillis());
                setAssemblerState(assembler, State.BLOCKED);
                belt.put(new Part(id, assembler));
                built.incrementAndGet();
                setAssemblerState(assembler, State.IDLE);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            setAssemblerState(assembler, State.DONE);
            assemblerFinished();
        }
    }

    private void packageParts(int packager) {
        try {
            while (true) {
                setPackagerState(packager, State.IDLE);
                Part part = belt.take();
                if (part == null) {
                    break;
                }
                totalBeltNanos.addAndGet(System.nanoTime() - part.beltEntryNanos());
                beltSamples.incrementAndGet();
                setPackagerState(packager, State.WORKING);
                Thread.sleep(workDelayMillis());
                packaged.incrementAndGet();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            setPackagerState(packager, State.DONE);
            packagerFinished();
        }
    }

    private synchronized void assemblerFinished() {
        if (++completedAssemblers == assemblerCount) {
            belt.completeProduction();
        }
    }

    private synchronized void packagerFinished() {
        if (++completedPackagers == packagerCount) {
            running = false;
            finishNanos = System.nanoTime();
        }
    }

    private long workDelayMillis() {
        return Math.max(40, Math.round(500 / speed));
    }

    private synchronized void setAssemblerState(int index, State state) {
        assemblerStates[index] = state;
    }

    private synchronized void setPackagerState(int index, State state) {
        packagerStates[index] = state;
    }

    public int assemblerCount() {
        return assemblerCount;
    }

    public int packagerCount() {
        return packagerCount;
    }

    public int totalParts() {
        return totalParts;
    }

    public ConveyorBelt getBelt() {
        return belt;
    }

    public int builtCount() {
        return built.get();
    }

    public int packagedCount() {
        return packaged.get();
    }

    public double avgBeltSeconds() {
        int samples = beltSamples.get();
        return samples == 0 ? 0 : totalBeltNanos.get() / samples / 1_000_000_000.0;
    }

    public double elapsedSeconds() {
        if (startNanos == 0) {
            return 0;
        }
        long end = finishNanos == 0 ? System.nanoTime() : finishNanos;
        return (end - startNanos) / 1_000_000_000.0;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isStopped() {
        return stopped;
    }

    public State assemblerState(int index) {
        synchronized (this) {
            return assemblerStates[index];
        }
    }

    public State packagerState(int index) {
        synchronized (this) {
            return packagerStates[index];
        }
    }
}
