import java.util.ArrayList;
import java.util.List;

public class Message {
    private final String content;
    private final int initialNodeIndex;
    private final List<Long> fullCircleTimestamps = new ArrayList<>();
    private long circleNumber = 1;

    public Message(String content, int initialNodeIndex) {
        this.content = content;
        this.initialNodeIndex = initialNodeIndex;
    }

    public void addTimestamp(Long timestamp) {
        fullCircleTimestamps.add(timestamp);
    }

    public String getContent() {
        return content;
    }

    public int getInitialNodeIndex() {
        return initialNodeIndex;
    }

    public List<Long> getFullCircleTimestamps() {
        return fullCircleTimestamps;
    }

    public List<Double> getFullCircleLatencies() {
        List<Double> result = new ArrayList<>();
        for (int i = 0; i < fullCircleTimestamps.size() - 1; i++) {
            result.add((double) (fullCircleTimestamps.get(i + 1) - fullCircleTimestamps.get(i)));
        }

        return result;
    }

    @Override
    public String toString() {
        return "Message{" +
                "content='" + content + '\'' +
                '}';
    }

    public void incrementCircleNumber() {
        circleNumber++;
    }

    public long getCircleNumber() {
        return circleNumber;
    }
}
