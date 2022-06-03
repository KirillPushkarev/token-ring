package tokenring;

import tokenring.messageprocessor.MessageProcessor;

import java.util.*;

public abstract class Node implements Runnable {
    private static final double NANOSECONDS_IN_SECOND = 1e+9;

    protected final int index;
    protected final MessageProcessor messageProcessor;
    private final int circlesBetweenLatencyRecord;
    private final List<Long> throughputValues = new ArrayList<>();
    protected long processedMessagesCounter = 0;
    protected Set<Message> registeredMessages = new HashSet<>();
    protected Message currentMessage;
    private long lastThroughputCollectionTimestamp;

    public Node(int index, MessageProcessor messageProcessor, int circlesBetweenLatencyRecord) {
        this.index = index;
        this.messageProcessor = messageProcessor;
        this.circlesBetweenLatencyRecord = circlesBetweenLatencyRecord;
    }

    public List<Long> getThroughputValues() {
        return throughputValues;
    }

    public abstract Collection<Message> getAssignedMessages();

    @Override
    public void run() {
        lastThroughputCollectionTimestamp = System.nanoTime();
    }

    protected void recordMetrics(Message message) {
        long currentTimestamp = System.nanoTime();

        processedMessagesCounter++;
        if (currentTimestamp - lastThroughputCollectionTimestamp >= NANOSECONDS_IN_SECOND) {
            recordThroughput(currentTimestamp);
        }

        if (!registeredMessages.contains(message)) {
            registeredMessages.add(message);
        } else if (message.getInitialNodeIndex() == index) {
            message.incrementCircleCounter();

            if (message.getCircleCounter() % circlesBetweenLatencyRecord == 0) {
                message.addTimestamp(currentTimestamp);
            }
        }
    }

    private void recordThroughput(long timestamp) {
        throughputValues.add(processedMessagesCounter);
        lastThroughputCollectionTimestamp = timestamp;
        processedMessagesCounter = 0;
    }
}
