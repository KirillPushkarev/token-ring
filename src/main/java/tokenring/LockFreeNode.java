package tokenring;

import tokenring.messageprocessor.MessageProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

public class LockFreeNode extends Node {
    private final Queue<Message> prevBuffer;
    private final Queue<Message> nextBuffer;

    public LockFreeNode(int index, Queue<Message> prevBuffer, Queue<Message> nextBuffer, MessageProcessor processingLogic, int circlesBetweenLatencyRecord) {
        super(index, processingLogic, circlesBetweenLatencyRecord);
        this.prevBuffer = prevBuffer;
        this.nextBuffer = nextBuffer;
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

            currentMessage = message;
            process(message);
            nextBuffer.add(message);
            currentMessage = null;

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
