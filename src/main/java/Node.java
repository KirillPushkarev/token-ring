import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class Node implements Runnable {
    protected final int index;
    protected final BiConsumer<Node, Message> processingLogic;
    private final int circlesBetweenLatencyRecord;
    private final List<Long> throughputValues = new ArrayList<>();
    protected long processedMessagesCounter = 0;
    private long lastTimestamp;

    public Node(int index, BiConsumer<Node, Message> processingLogic, int circlesBetweenLatencyRecord) {
        this.index = index;
        this.processingLogic = processingLogic;
        this.circlesBetweenLatencyRecord = circlesBetweenLatencyRecord;
    }

    public abstract Collection<Message> getNextBuffer();

    public List<Long> getThroughputValues() {
        return throughputValues;
    }

    @Override
    public void run() {
        lastTimestamp = System.nanoTime();
    }

    protected void recordMetrics(Message message) {
        long timestamp = System.nanoTime();
        if (timestamp - lastTimestamp >= 1e+9) {
            throughputValues.add(processedMessagesCounter);
            lastTimestamp = timestamp;
            processedMessagesCounter = 0;
        }

        processedMessagesCounter++;
        if (message.getInitialNodeIndex() == index && message.getCircleNumber() % circlesBetweenLatencyRecord == 0) {
            message.addTimestamp(timestamp);
        }
        message.incrementCircleNumber();
    }
}
