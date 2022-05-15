import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;

public class BlockingNode extends Node {
    public final static int BUFFER_CAPACITY = 1000;

    private final BlockingQueue<Message> prevBuffer;
    private final BlockingQueue<Message> nextBuffer;

    public BlockingNode(int index, BlockingQueue<Message> prevBuffer, BlockingQueue<Message> nextBuffer, BiConsumer<Node, Message> processingLogic, int circlesBetweenLatencyRecord) {
        super(index, processingLogic, circlesBetweenLatencyRecord);
        this.prevBuffer = prevBuffer;
        this.nextBuffer = nextBuffer;
    }

    @Override
    public Collection<Message> getNextBuffer() {
        return nextBuffer;
    }

    @Override
    public void run() {
        super.run();

        while (true) {
            try {
                Message message = prevBuffer.take();
                process(message);
                nextBuffer.put(message);
            } catch (InterruptedException e) {
                return;
            }

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
