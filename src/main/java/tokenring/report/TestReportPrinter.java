package tokenring.report;

import tokenring.MetricCollector;
import tokenring.TestConfig;

import java.util.Formatter;
import java.util.Locale;

public class TestReportPrinter {
    public static final String NUMBER_FORMAT = "%6.3E";

    public void printReport(String testName, TestConfig testConfig, MetricCollector.TestMetrics metrics) {
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println("Finished running test with name: " + testName + ", with config: " + testConfig);
        System.out.println("Test result report for test: " + testName + ".");
        System.out.println("Latency per circle:");
        System.out.println("----- avg.: " + String.format(NUMBER_FORMAT, metrics.getCircleLatencyAvg()) + " seconds.");
        System.out.println("----- median: " + String.format(NUMBER_FORMAT, metrics.getCircleLatencyMedian()) + " seconds.");
        System.out.println("----- p90: " + String.format(NUMBER_FORMAT, metrics.getCircleLatencyPercentile90()) + " seconds.");
        System.out.println("Throughput per second:");
        System.out.println("----- avg.: " + String.format(NUMBER_FORMAT, metrics.getThroughputPerSecondAvg()) + " messages.");
        System.out.println("----- median: " + String.format(NUMBER_FORMAT, metrics.getThroughputPerSecondMedian()) + " messages.");
        System.out.println("----- p90: " + String.format(NUMBER_FORMAT, metrics.getThroughputPerSecondPercentile90()) + " messages.");
        System.out.println("-------------------------------------------------------------------------------------");
    }
}
