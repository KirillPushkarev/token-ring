package tokenring;

import tokenring.messageprocessor.MessageProcessor;
import tokenring.messageprocessor.VoidMessageProcessor;
import tokenring.report.SummaryReportPrinter;
import tokenring.report.TestReportPrinter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TokenRing {
    public static final int BUFFER_CAPACITY = 100;
    private static final int WARM_UP_NODES = 2;
    private static final int WARM_UP_MESSAGES = 4;
    private static final int WARM_UP_RUNNING_TIME_IN_SECONDS = 10;
    private static final int TEST_RUNNING_TIME_IN_SECONDS = 30;
    private static final int CIRCLES_BETWEEN_LATENCY_RECORD = 100;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new RuntimeException("Expected arguments: test name.");
        }

        String testName = args[0];
        Locale.setDefault(Locale.US);

        warmUp(new TestConfig(WARM_UP_NODES, WARM_UP_MESSAGES));
        runTests(testName);
    }

    private static void warmUp(TestConfig testConfig) {
        List<BlockingNode> tokenRingWithBlockingBuffers = createTokenRingWithBlockingBuffers(
                testConfig.getNumberOfNodes(),
                testConfig.getNumberOfMessages(),
                TokenRing::getArrayBlockingBuffer,
                new VoidMessageProcessor()
        );
        runThreadsWithTimeout(tokenRingWithBlockingBuffers, WARM_UP_RUNNING_TIME_IN_SECONDS);

        List<LockFreeNode> tokenRingWithLockFreeBuffers = createTokenRingWithLockFreeBuffers(
                testConfig.getNumberOfNodes(),
                testConfig.getNumberOfMessages(),
                TokenRing::getLockFreeBuffer,
                new VoidMessageProcessor()
        );
        runThreadsWithTimeout(tokenRingWithLockFreeBuffers, WARM_UP_RUNNING_TIME_IN_SECONDS);

        System.out.println("Finished warm-up");
    }

    private static void runTests(String testName) throws IOException {
        List<TestConfig> testConfigs = List.of(
                new TestConfig(4, 1),
                new TestConfig(4, 2),
                new TestConfig(4, 3),
                new TestConfig(4, 4),
                new TestConfig(4, 6),
                new TestConfig(4, 8),
                new TestConfig(4, 16),
                new TestConfig(4, 24),
                new TestConfig(4, 32),
                new TestConfig(4, 48),
                new TestConfig(4, 64),
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

    private static void runWithTestConfigs(TestRunner testRunner, List<TestConfig> testConfigs, String testName) throws IOException {
        MetricCollector metricCollector = new MetricCollector(CIRCLES_BETWEEN_LATENCY_RECORD);
        TestReportPrinter testReportPrinter = new TestReportPrinter();
        SummaryReportPrinter summaryReportPrinter = new SummaryReportPrinter();

        for (TestConfig testConfig : testConfigs) {
            testRunner.run(testConfig, metricCollector);
            testReportPrinter.printReport(testName, testConfig, metricCollector.getLastTestMetrics());
        }

        summaryReportPrinter.printReport(metricCollector, testName, testConfigs);
    }

    private static void testWithArrayBlockingQueue(TestConfig testConfig, MetricCollector metricCollector) {
        List<BlockingNode> nodes = createTokenRingWithBlockingBuffers(
                testConfig.getNumberOfNodes(),
                testConfig.getNumberOfMessages(),
                TokenRing::getArrayBlockingBuffer,
                new VoidMessageProcessor()
        );

        runThreadsWithTimeout(nodes, TEST_RUNNING_TIME_IN_SECONDS);
        metricCollector.addMetrics(nodes);
    }

    private static void testWithLinkedBlockingQueue(TestConfig testConfig, MetricCollector metricCollector) {
        List<BlockingNode> nodes = createTokenRingWithBlockingBuffers(
                testConfig.getNumberOfNodes(),
                testConfig.getNumberOfMessages(),
                TokenRing::getLinkedBlockingBuffer,
                new VoidMessageProcessor()
        );

        runThreadsWithTimeout(nodes, TEST_RUNNING_TIME_IN_SECONDS);
        metricCollector.addMetrics(nodes);
    }

    private static void testWithLinkedConcurrentQueue(TestConfig testConfig, MetricCollector metricCollector) {
        List<LockFreeNode> nodes = createTokenRingWithLockFreeBuffers(
                testConfig.getNumberOfNodes(),
                testConfig.getNumberOfMessages(),
                TokenRing::getLockFreeBuffer,
                new VoidMessageProcessor()
        );

        runThreadsWithTimeout(nodes, TEST_RUNNING_TIME_IN_SECONDS);
        metricCollector.addMetrics(nodes);
    }

    private static List<BlockingNode> createTokenRingWithBlockingBuffers(int numberOfNodes,
                                                                         int numberOfMessages,
                                                                         Supplier<BlockingQueue<Message>> bufferFactory,
                                                                         MessageProcessor messageProcessor) {
        BlockingQueue<Message> prevBuffer = bufferFactory.get();
        BlockingQueue<Message> firstNodePrevBuffer = prevBuffer;
        BlockingQueue<Message> nextBuffer;
        List<BlockingNode> nodes = new ArrayList<>();
        int messageCounter = 0;

        for (int i = 0; i < numberOfNodes; i++) {
            boolean isLastNode = i == numberOfNodes - 1;
            nextBuffer = !isLastNode ? bufferFactory.get() : firstNodePrevBuffer;
            int numberOfMessagesPerNode = numberOfMessages / numberOfNodes + (i < numberOfMessages % numberOfNodes ? 1 : 0);
            for (int j = 0; j < numberOfMessagesPerNode; j++) {
                try {
                    nextBuffer.put(new Message("Initial node number:" + i + ". tokenring.Message number: " + (messageCounter + j), i));
                } catch (InterruptedException e) {
                }
            }
            messageCounter += numberOfMessagesPerNode;
            BlockingNode node = new BlockingNode(i, prevBuffer, nextBuffer, messageProcessor, CIRCLES_BETWEEN_LATENCY_RECORD);
            nodes.add(node);
            prevBuffer = nextBuffer;
        }

        return nodes;
    }

    private static List<LockFreeNode> createTokenRingWithLockFreeBuffers(int numberOfNodes,
                                                                         int numberOfMessages,
                                                                         Supplier<Queue<Message>> bufferFactory,
                                                                         MessageProcessor messageProcessor) {
        Queue<Message> prevBuffer = bufferFactory.get();
        Queue<Message> firstNodePrevBuffer = prevBuffer;
        Queue<Message> nextBuffer;
        List<LockFreeNode> nodes = new ArrayList<>();
        int messageCounter = 0;

        for (int i = 0; i < numberOfNodes; i++) {
            boolean isLastNode = i == numberOfNodes - 1;
            nextBuffer = !isLastNode ? bufferFactory.get() : firstNodePrevBuffer;
            int numberOfMessagesPerNode = numberOfMessages / numberOfNodes + (i < numberOfMessages % numberOfNodes ? 1 : 0);
            for (int j = 0; j < numberOfMessagesPerNode; j++) {
                nextBuffer.add(new Message("Initial node number:" + i + ". tokenring.Message number: " + (messageCounter + j), i));
            }
            messageCounter += numberOfMessagesPerNode;
            LockFreeNode node = new LockFreeNode(i, prevBuffer, nextBuffer, messageProcessor, CIRCLES_BETWEEN_LATENCY_RECORD);
            nodes.add(node);
            prevBuffer = nextBuffer;
        }

        return nodes;
    }

    private static ArrayBlockingQueue<Message> getArrayBlockingBuffer() {
        return new ArrayBlockingQueue<>(BUFFER_CAPACITY);
    }

    private static LinkedBlockingQueue<Message> getLinkedBlockingBuffer() {
        return new LinkedBlockingQueue<>(BUFFER_CAPACITY);
    }

    private static ConcurrentLinkedQueue<Message> getLockFreeBuffer() {
        return new ConcurrentLinkedQueue<>();
    }

    private static void runThreadsWithTimeout(List<? extends Node> nodes, int timeoutInSeconds) {
        List<Thread> threads = nodes.stream().map(Thread::new).collect(Collectors.toList());
        Timer timer = new Timer();
        for (Thread thread : threads) {
            thread.start();
            TimeOutTask timeOutTask = new TimeOutTask(thread);
            timer.schedule(timeOutTask, timeoutInSeconds * 1000L);
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
        }

        timer.cancel();
    }
}
