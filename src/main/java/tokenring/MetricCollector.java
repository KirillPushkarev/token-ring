package tokenring;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.ArrayList;
import java.util.List;

public class MetricCollector {
    private static final double NANOSECONDS_IN_SECOND = 1e+9;
    private final List<TestMetrics> metrics = new ArrayList<>();
    private final int circlesBetweenLatencyRecord;

    public MetricCollector(int circlesBetweenLatencyRecord) {
        this.circlesBetweenLatencyRecord = circlesBetweenLatencyRecord;
    }

    public void addMetrics(List<? extends Node> nodes) {
        List<Message> messages = new ArrayList<>();
        for (Node node : nodes) {
            messages.addAll(node.getAssignedMessages());
        }

        Median median = new Median();
        Percentile percentile90 = new Percentile(90);

        double cumulativeCircleLatency = 0;
        List<Double> circleLatencies = new ArrayList<>();
        for (Message message : messages) {
            List<Double> fullCircleLatencies = message.getFullCircleLatencies();
            cumulativeCircleLatency += fullCircleLatencies.stream().parallel().reduce(Double::sum).orElseThrow();
            circleLatencies.addAll(fullCircleLatencies);
        }
        double circleLatencyAvg = cumulativeCircleLatency / circleLatencies.size() / circlesBetweenLatencyRecord / NANOSECONDS_IN_SECOND;
        double circleLatencyMedian = median.evaluate(circleLatencies.stream().mapToDouble(v -> v).toArray()) / circlesBetweenLatencyRecord / NANOSECONDS_IN_SECOND;
        double circleLatencyPercentile90 = percentile90.evaluate(circleLatencies.stream().mapToDouble(v -> v).toArray()) / circlesBetweenLatencyRecord / NANOSECONDS_IN_SECOND;

        long cumulativeThroughput = 0;
        List<Long> throughputPerValues = new ArrayList<>();
        for (Node node : nodes) {
            cumulativeThroughput += node.getThroughputValues().stream().parallel().reduce(Long::sum).orElseThrow();
            throughputPerValues.addAll(node.getThroughputValues());
        }
        double throughputPerSecondAvg = (double)cumulativeThroughput / throughputPerValues.size();
        double throughputPerSecondMedian = median.evaluate(throughputPerValues.stream().mapToDouble(v -> v).toArray());
        double throughputPerSecondPercentile90 = percentile90.evaluate(throughputPerValues.stream().mapToDouble(v -> v).toArray());

        metrics.add(
                new TestMetrics(
                        circleLatencyMedian,
                        circleLatencyAvg,
                        circleLatencyPercentile90,
                        throughputPerSecondAvg,
                        throughputPerSecondMedian,
                        throughputPerSecondPercentile90
                )
        );
    }

    public List<TestMetrics> getMetrics() {
        return metrics;
    }

    public TestMetrics getLastTestMetrics() {
        return metrics.get(metrics.size() - 1);
    }

    public static class TestMetrics {
        private final double circleLatencyMedian;
        private final double circleLatencyAvg;
        private final double circleLatencyPercentile90;
        private final double throughputPerSecondAvg;
        private final double throughputPerSecondMedian;
        private final double throughputPerSecondPercentile90;

        public TestMetrics(
                double circleLatencyMedian,
                double circleLatencyAvg,
                double circleLatencyPercentile90,
                double throughputPerSecondAvg,
                double throughputPerSecondMedian,
                double throughputPerSecondPercentile90) {

            this.circleLatencyMedian = circleLatencyMedian;
            this.circleLatencyAvg = circleLatencyAvg;
            this.circleLatencyPercentile90 = circleLatencyPercentile90;
            this.throughputPerSecondAvg = throughputPerSecondAvg;
            this.throughputPerSecondMedian = throughputPerSecondMedian;
            this.throughputPerSecondPercentile90 = throughputPerSecondPercentile90;
        }

        public double getCircleLatencyMedian() {
            return circleLatencyMedian;
        }

        public double getCircleLatencyAvg() {
            return circleLatencyAvg;
        }

        public double getCircleLatencyPercentile90() {
            return circleLatencyPercentile90;
        }

        public double getThroughputPerSecondAvg() {
            return throughputPerSecondAvg;
        }

        public double getThroughputPerSecondMedian() {
            return throughputPerSecondMedian;
        }

        public double getThroughputPerSecondPercentile90() {
            return throughputPerSecondPercentile90;
        }
    }
}
