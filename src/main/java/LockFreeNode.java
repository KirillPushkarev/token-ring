import java.util.Collection;
import java.util.Queue;
import java.util.function.BiConsumer;

public class LockFreeNode extends Node {
    private final Queue<Message> prevBuffer;
    private final Queue<Message> nextBuffer;

    public LockFreeNode(int index, Queue<Message> prevBuffer, Queue<Message> nextBuffer, BiConsumer<Node, Message> processingLogic, int circlesBetweenLatencyRecord) {
        super(index, processingLogic, circlesBetweenLatencyRecord);
        this.prevBuffer = prevBuffer;
        this.nextBuffer = nextBuffer;
    }

    public Collection<Message> getNextBuffer() {
        return nextBuffer;
    }

    public long getProcessedMessagesCounter() {
        return processedMessagesCounter;
    }

    @Override
    public void run() {
        super.run();

        while (true) {
            Message message = null;
            while (message == null) {
                message = prevBuffer.poll();

                if (Thread.interrupted()) {
                    return;
                }
            }
            process(message);
            nextBuffer.add(message);

            if (Thread.interrupted()) {
                return;
            }
        }
    }

    private void process(Message message) {
        recordMetrics(message);

        processingLogic.accept(this, message);
    }
}
