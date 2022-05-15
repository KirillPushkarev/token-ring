import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.ArrayList;
import java.util.List;

public class MetricCollector {
    private final List<TestMetrics> metrics = new ArrayList<>();

    public void addMetrics(List<? extends Node> nodes) {
        List<Message> messages = new ArrayList<>();
        for (Node node : nodes) {
            messages.addAll(node.getNextBuffer());
        }

        Median median = new Median();
        Percentile percentile90 = new Percentile(90);

        double cumulativeCircleLatency = 0;
        List<Double> circleLatencies = new ArrayList<>();
        for (Message message : messages) {
            cumulativeCircleLatency += message.getFullCircleLatencies().stream().reduce(Double::sum).orElseThrow();
            circleLatencies.addAll(message.getFullCircleLatencies());
        }
        double circleLatencyAvg = cumulativeCircleLatency / circleLatencies.size() / 1e+9;
        double circleLatencyMedian = median.evaluate(circleLatencies.stream().mapToDouble(v -> v).toArray()) / 1e+9;
        double circleLatencyPercentile90 = percentile90.evaluate(circleLatencies.stream().mapToDouble(v -> v).toArray()) / 1e+9;

        long cumulativeThroughput = 0;
        List<Long> throughputPerSecondValues = new ArrayList<>();
        for (Node node : nodes) {
            cumulativeThroughput += node.getThroughputValues().stream().reduce(Long::sum).orElseThrow();
            throughputPerSecondValues.addAll(node.getThroughputValues());
        }
        double throughputPerSecondAvg = 1.0 * cumulativeThroughput / throughputPerSecondValues.size();
        double throughputPerSecondMedian = median.evaluate(throughputPerSecondValues.stream().mapToDouble(v -> v).toArray());
        double throughputPerSecondPercentile90 = percentile90.evaluate(throughputPerSecondValues.stream().mapToDouble(v -> v).toArray());

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

    public TestMetrics getLastMetrics() {
        return metrics.get(metrics.size() - 1);
    }

    public List<TestMetrics> getMetrics() {
        return metrics;
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
