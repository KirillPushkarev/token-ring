package tokenring.report;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import tokenring.MetricCollector;
import tokenring.TestConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SummaryReportPrinter {
    private final static String BASE_DIRECTORY_NAME = "output";
    private final static String BASE_FILE_NAME = "results";

    public void printReport(MetricCollector metricCollector, String testName, List<TestConfig> testConfigs) throws IOException {
        saveResultsToFile(metricCollector, testName, testConfigs);
    }

    private void saveResultsToFile(MetricCollector metricCollector, String testName, List<TestConfig> testConfigs) throws IOException {
        String[] HEADERS = {"Number of nodes", "Number of messages", "Median latency", "Median throughput"};
        String fileNameSuffix = "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".csv";
        String fileName = BASE_DIRECTORY_NAME + File.separator + testName + "_" + BASE_FILE_NAME + fileNameSuffix;

        FileWriter out = new FileWriter(fileName);
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(HEADERS))) {
            List<MetricCollector.TestMetrics> metrics = metricCollector.getMetrics();
            for (int i = 0; i < metrics.size(); i++) {
                TestConfig testConfig = testConfigs.get(i);
                MetricCollector.TestMetrics testMetrics = metrics.get(i);
                printer.printRecord(testConfig.getNumberOfNodes(), testConfig.getNumberOfMessages(), testMetrics.getCircleLatencyMedian(), testMetrics.getThroughputPerSecondMedian());
            }
        }

        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println("Finished running test with name: " + testName);
        System.out.println("Test metrics saved in file: " + fileName);
        System.out.println("-------------------------------------------------------------------------------------");
    }
}
