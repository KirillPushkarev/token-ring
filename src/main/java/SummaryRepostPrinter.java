import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;

public class SummaryRepostPrinter {
    private final static String FILE_NAME = "results.csv";

    public void printReport(MetricCollector metricCollector, String testName) throws IOException {
        saveResultsInFile(metricCollector, testName);
    }

    private void saveResultsInFile(MetricCollector metricCollector, String testName) throws IOException {
        String[] HEADERS = {"Median latency", "Median throughput"};
        FileWriter out = new FileWriter(testName + "_" + FILE_NAME);
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(HEADERS))) {
            for (MetricCollector.TestMetrics testMetrics : metricCollector.getMetrics()) {
                printer.printRecord(testMetrics.getCircleLatencyMedian(), testMetrics.getThroughputPerSecondMedian());
            }
        }
    }
}
