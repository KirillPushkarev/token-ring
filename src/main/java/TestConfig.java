public class TestConfig {
    private final int numberOfNodes;
    private final int numberOfMessages;

    public TestConfig(int numberOfNodes, int numberOfMessages) {
        this.numberOfNodes = numberOfNodes;
        this.numberOfMessages = numberOfMessages;
    }

    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    public int getNumberOfMessages() {
        return numberOfMessages;
    }
}
