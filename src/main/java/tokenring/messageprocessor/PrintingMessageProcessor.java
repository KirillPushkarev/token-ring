package tokenring.messageprocessor;

import tokenring.Message;
import tokenring.Node;

public class PrintingMessageProcessor implements MessageProcessor {
    public void processMessage(Node node, Message message) {
        System.out.println("Processing message: " + message);
    }
}
