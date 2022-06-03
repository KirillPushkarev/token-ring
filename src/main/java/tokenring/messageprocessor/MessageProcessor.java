package tokenring.messageprocessor;

import tokenring.Message;
import tokenring.Node;

public interface MessageProcessor {
    void processMessage(Node node, Message message);
}
