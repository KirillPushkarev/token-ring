import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class SummaryRepostPrinter {
    private final static String FILE_NAME = "results.csv";

    public void printReport(MetricCollector metricCollector, String testName, List<TestConfig> testConfigs) throws IOException {
        saveResultsInFile(metricCollector, testName, testConfigs);
    }

    private void saveResultsInFile(MetricCollector metricCollector, String testName, List<TestConfig> testConfigs) throws IOException {
        String[] HEADERS = {"Number of nodes", "Number of messages", "Median latency", "Median throughput"};
        FileWriter out = new FileWriter(testName + "_" + FILE_NAME);
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(HEADERS))) {
            List<MetricCollector.TestMetrics> metrics = metricCollector.getMetrics();
            for (int i = 0; i < metrics.size(); i++) {
                TestConfig testConfig = testConfigs.get(i);
                MetricCollector.TestMetrics testMetrics = metrics.get(i);
                printer.printRecord(testConfig.getNumberOfNodes(), testConfig.getNumberOfMessages(), testMetrics.getCircleLatencyMedian(), testMetrics.getThroughputPerSecondMedian());
            }
        }
    }
}
