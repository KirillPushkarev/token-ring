import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestReportPrinter {
    public void printReport(MetricCollector.TestMetrics metrics, String testName) {
        System.out.println("Test result report for test: " + testName + ".");
        System.out.println("Latency per circle:");
        System.out.println("----- avg.: " + metrics.getCircleLatencyAvg() + " seconds.");
        System.out.println("----- median: " + metrics.getCircleLatencyMedian() + " seconds.");
        System.out.println("----- p90: " + metrics.getCircleLatencyPercentile90() + " seconds.");
        System.out.println("Throughput per second:");
        System.out.println("----- avg.: " + metrics.getThroughputPerSecondAvg() + " messages.");
        System.out.println("----- median: " + metrics.getThroughputPerSecondMedian() + " messages.");
        System.out.println("----- p90: " + metrics.getThroughputPerSecondPercentile90() + " messages.");
        System.out.println("-------------------------------------------------------------------------------------");
    }
}
