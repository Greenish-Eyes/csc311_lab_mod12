package factory;

record Part(int id, int assembler, long beltEntryNanos) {
    Part(int id, int assembler) {
        this(id, assembler, 0);
    }

    Part enteringBelt() {
        return new Part(id, assembler, System.nanoTime());
    }
}
