package CLHUIMiner;

public class MemoryLogger {
    private static MemoryLogger instance = new MemoryLogger();
    private long maxMemory = 0;

    private MemoryLogger() {}

    public static MemoryLogger getInstance() {
        return instance;
    }

    public void checkMemory() {
        long memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        if (memory > maxMemory) {
            maxMemory = memory;
        }
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    public void reset() {
        maxMemory = 0;
    }
}
