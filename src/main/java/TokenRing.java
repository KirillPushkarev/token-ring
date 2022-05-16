import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TokenRing {
    private static final int WARM_UP_NODES = 2;
    private static final int WARM_UP_MESSAGES = 4;
    private static final int WARM_UP_RUNNING_TIME_IN_SECONDS = 10;
    private static final int TEST_RUNNING_TIME_IN_SECONDS = 60;
    private static final int CIRCLES_BETWEEN_LATENCY_RECORD = 1000;
    private static final String[] SUPPORTED_TESTS = new String[]{"ArrayBlockingQueue", "LinkedBlockingQueue", "LinkedConcurrentQueue"};

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new RuntimeException("Expected arguments: test name.");
        }

        String testName = args[0];

        warmUpVirtualMachine(new TestConfig(WARM_UP_NODES, WARM_UP_MESSAGES));
        List<TestConfig> testConfigs = List.of(
                new TestConfig(4, 2),
                new TestConfig(4, 4),
                new TestConfig(4, 16),
                new TestConfig(4, 128)
        );
        switch (testName) {
            case "ArrayBlockingQueue":
                runWithTestConfigs(TokenRing::testWithArrayBlockingQueue, testConfigs, testName);
                break;
            case "LinkedBlockingQueue":
                runWithTestConfigs(TokenRing::testWithLinkedBlockingQueue, testConfigs, testName);
                break;
            case "LinkedConcurrentQueue":
                runWithTestConfigs(TokenRing::testWithLinkedConcurrentQueue, testConfigs, testName);
                break;
            default:
                throw new RuntimeException("Unsupported test name.");
        }
    }

    private static void warmUpVirtualMachine(TestConfig testConfig) {
        List<BlockingNode> blockingNodes = getBlockingNodes(testConfig.getNumberOfNodes(),
                testConfig.getNumberOfMessages(),
                TokenRing::getArrayBlockingBuffer,
                TokenRing::processMessageDoNothing);
        runThreadsWithTimeout(blockingNodes, WARM_UP_RUNNING_TIME_IN_SECONDS);

        List<LockFreeNode> lockFreeNodes = getLockFreeNodes(testConfig.getNumberOfNodes(),
                testConfig.getNumberOfMessages(),
                TokenRing::getLockFreeBuffer,
                TokenRing::processMessageDoNothing);
        runThreadsWithTimeout(lockFreeNodes, WARM_UP_RUNNING_TIME_IN_SECONDS);
    }

    private static void runThreadsWithTimeout(List<? extends Node> nodes, int timeoutInSeconds) {
        List<Thread> threads = nodes.stream().map(Thread::new).collect(Collectors.toList());
        for (Thread thread : threads) {
            thread.start();
            Timer timer = new Timer();
            TimeOutTask timeOutTask = new TimeOutTask(thread, timer);
            timer.schedule(timeOutTask, timeoutInSeconds * 1000L);
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
        }
    }

    private static void runWithTestConfigs(BiConsumer<TestConfig, MetricCollector> test, List<TestConfig> testConfigs, String testName) throws IOException {
        MetricCollector metricCollector = new MetricCollector(CIRCLES_BETWEEN_LATENCY_RECORD);
        for (TestConfig testConfig : testConfigs) {
            test.accept(testConfig, metricCollector);
        }

        new SummaryRepostPrinter().printReport(metricCollector, testName, testConfigs);
    }

    private static void testWithArrayBlockingQueue(TestConfig testConfig, MetricCollector metricCollector) {
        List<BlockingNode> nodes = getBlockingNodes(testConfig.getNumberOfNodes(),
                testConfig.getNumberOfMessages(),
                TokenRing::getArrayBlockingBuffer,
                TokenRing::processMessageDoNothing);
        runThreadsWithTimeout(nodes, TEST_RUNNING_TIME_IN_SECONDS);

        metricCollector.addMetrics(nodes);
        new TestReportPrinter().printReport(metricCollector.getLastMetrics(), "ArrayBlockingQueue");
    }

    private static void testWithLinkedBlockingQueue(TestConfig testConfig, MetricCollector metricCollector) {
        List<BlockingNode> nodes = getBlockingNodes(testConfig.getNumberOfNodes(),
                testConfig.getNumberOfMessages(),
                TokenRing::getLinkedBlockingBuffer,
                TokenRing::processMessageDoNothing);
        runThreadsWithTimeout(nodes, TEST_RUNNING_TIME_IN_SECONDS);

        metricCollector.addMetrics(nodes);
        new TestReportPrinter().printReport(metricCollector.getLastMetrics(), "LinkedBlockingQueue");
    }

    private static void testWithLinkedConcurrentQueue(TestConfig testConfig, MetricCollector metricCollector) {
        List<LockFreeNode> nodes = getLockFreeNodes(testConfig.getNumberOfNodes(),
                testConfig.getNumberOfMessages(),
                TokenRing::getLockFreeBuffer,
                TokenRing::processMessageDoNothing);
        runThreadsWithTimeout(nodes, TEST_RUNNING_TIME_IN_SECONDS);

        metricCollector.addMetrics(nodes);
        new TestReportPrinter().printReport(metricCollector.getLastMetrics(), "LinkedConcurrentQueue");
    }

    private static List<BlockingNode> getBlockingNodes(int numberOfNodes,
                                                       int numberOfMessages,
                                                       Supplier<BlockingQueue<Message>> bufferFactory,
                                                       BiConsumer<Node, Message> processingLogic) {
        BlockingQueue<Message> prevBuffer = bufferFactory.get();
        BlockingQueue<Message> firstNodePrevBuffer = prevBuffer;
        BlockingQueue<Message> nextBuffer;
        List<BlockingNode> nodes = new ArrayList<>();
        int messageCounter = 0;

        for (int i = 0; i < numberOfNodes; i++) {
            nextBuffer = i < numberOfNodes - 1 ? bufferFactory.get() : firstNodePrevBuffer;
            int numberOfMessagesPerNode = numberOfMessages / numberOfNodes + (i < numberOfMessages % numberOfNodes ? 1 : 0);
            for (int j = 0; j < numberOfMessagesPerNode; j++) {
                try {
                    nextBuffer.put(new Message("Initial node number:" + i + ". Message number: " + (messageCounter + j), i));
                } catch (InterruptedException e) {
                }
            }
            messageCounter += numberOfMessagesPerNode;
            BlockingNode node = new BlockingNode(i, prevBuffer, nextBuffer, processingLogic, CIRCLES_BETWEEN_LATENCY_RECORD);
            nodes.add(node);
            prevBuffer = nextBuffer;
        }

        return nodes;
    }

    private static List<LockFreeNode> getLockFreeNodes(int numberOfNodes,
                                                       int numberOfMessages,
                                                       Supplier<Queue<Message>> bufferFactory,
                                                       BiConsumer<Node, Message> processingLogic) {
        Queue<Message> prevBuffer = bufferFactory.get();
        Queue<Message> firstNodePrevBuffer = prevBuffer;
        Queue<Message> nextBuffer;
        List<LockFreeNode> nodes = new ArrayList<>();
        int messageCounter = 0;

        for (int i = 0; i < numberOfNodes; i++) {
            nextBuffer = i < numberOfNodes - 1 ? bufferFactory.get() : firstNodePrevBuffer;
            int numberOfMessagesPerNode = numberOfMessages / numberOfNodes + (i < numberOfMessages % numberOfNodes ? 1 : 0);
            for (int j = 0; j < numberOfMessagesPerNode; j++) {
                nextBuffer.add(new Message("Initial node number:" + i + ". Message number: " + (messageCounter + j), i));
            }
            messageCounter += numberOfMessagesPerNode;
            LockFreeNode node = new LockFreeNode(i, prevBuffer, nextBuffer, processingLogic, CIRCLES_BETWEEN_LATENCY_RECORD);
            nodes.add(node);
            prevBuffer = nextBuffer;
        }

        return nodes;
    }

    private static ArrayBlockingQueue<Message> getArrayBlockingBuffer() {
        return new ArrayBlockingQueue<>(BlockingNode.BUFFER_CAPACITY);
    }

    private static LinkedBlockingQueue<Message> getLinkedBlockingBuffer() {
        return new LinkedBlockingQueue<>(BlockingNode.BUFFER_CAPACITY);
    }

    private static ConcurrentLinkedQueue<Message> getLockFreeBuffer() {
        return new ConcurrentLinkedQueue<>();
    }

    private static void processMessageDoNothing(Node node, Message message) {
    }

    private static void processMessagePrintContent(Node node, Message message) {
        System.out.println("Processing message: " + message);
    }
}
