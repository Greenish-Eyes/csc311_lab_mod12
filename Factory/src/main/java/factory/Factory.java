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
    private final ConveyorBelt inputBelt;
    private final ConveyorBelt packagingBelt;
    private final State[] assemblerStates;
    private final State[] packagerStates;
    private final Part[] packagerParts;
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
        inputBelt = new ConveyorBelt(beltCapacity);
        packagingBelt = new ConveyorBelt(beltCapacity);
        assemblerStates = new State[assemblerCount];
        packagerStates = new State[packagerCount];
        packagerParts = new Part[packagerCount];
        workers = new Thread[assemblerCount + packagerCount + 1];
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
        workers[workerIndex++] = new Thread(this::supplyParts, "factory-parts-supplier");
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
        inputBelt.completeProduction();
        packagingBelt.completeProduction();
    }

    private void supplyParts() {
        try {
            for (int id = 1; id <= totalParts; id++) {
                Thread.sleep(workDelayMillis());
                inputBelt.put(new Part(id, -1));
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            inputBelt.completeProduction();
        }
    }

    private void assembleParts(int assembler) {
        try {
            while (true) {
                setAssemblerState(assembler, State.IDLE);
                Part rawPart = inputBelt.take(beltTravelNanos());
                if (rawPart == null) {
                    break;
                }
                setAssemblerState(assembler, State.WORKING);
                Thread.sleep(workDelayMillis());
                setAssemblerState(assembler, State.BLOCKED);
                packagingBelt.put(new Part(rawPart.id(), assembler));
                built.incrementAndGet();
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
                Part part = packagingBelt.take(beltTravelNanos());
                if (part == null) {
                    break;
                }
                totalBeltNanos.addAndGet(System.nanoTime() - part.beltEntryNanos());
                beltSamples.incrementAndGet();
                setPackagerPart(packager, part);
                setPackagerState(packager, State.WORKING);
                Thread.sleep(workDelayMillis());
                packaged.incrementAndGet();
                setPackagerPart(packager, null);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            setPackagerPart(packager, null);
            setPackagerState(packager, State.DONE);
            packagerFinished();
        }
    }

    private synchronized void assemblerFinished() {
        if (++completedAssemblers == assemblerCount) {
            packagingBelt.completeProduction();
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

    private long beltTravelNanos() {
        return Math.max(600, Math.round(1_200 / speed)) * 1_000_000;
    }

    public long beltTravelMillis() {
        return beltTravelNanos() / 1_000_000;
    }

    private synchronized void setAssemblerState(int index, State state) {
        assemblerStates[index] = state;
    }

    private synchronized void setPackagerState(int index, State state) {
        packagerStates[index] = state;
    }

    private synchronized void setPackagerPart(int index, Part part) {
        packagerParts[index] = part;
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

    public ConveyorBelt getInputBelt() {
        return inputBelt;
    }

    public ConveyorBelt getPackagingBelt() {
        return packagingBelt;
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

    public Part packagerPart(int index) {
        synchronized (this) {
            return packagerParts[index];
        }
    }
}
