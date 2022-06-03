package tokenring;

import tokenring.messageprocessor.MessageProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class BlockingNode extends Node {
    private final BlockingQueue<Message> prevBuffer;
    private final BlockingQueue<Message> nextBuffer;

    public BlockingNode(int index, BlockingQueue<Message> prevBuffer, BlockingQueue<Message> nextBuffer, MessageProcessor processingLogic, int circlesBetweenLatencyRecord) {
        super(index, processingLogic, circlesBetweenLatencyRecord);
        this.prevBuffer = prevBuffer;
        this.nextBuffer = nextBuffer;
    }

    @Override
    public void run() {
        super.run();

        while (true) {
            try {
                Message message = prevBuffer.take();
                currentMessage = message;
                process(message);
                nextBuffer.put(message);
                currentMessage = null;
            } catch (InterruptedException e) {
                return;
            }

            if (Thread.interrupted()) {
                return;
            }
        }
    }

    private void process(Message message) {
        messageProcessor.processMessage(this, message);
        recordMetrics(message);
    }

    @Override
    public Collection<Message> getAssignedMessages() {
        List<Message> assignedMessages = new ArrayList<>(prevBuffer);
        if (currentMessage != null) {
            assignedMessages.add(currentMessage);
        }

        return assignedMessages;
    }
}
