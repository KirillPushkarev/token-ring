package tokenring;

import java.util.ArrayList;
import java.util.List;

public class Message {
    private final String content;
    private final int initialNodeIndex;
    private final List<Long> fullCircleTimestamps = new ArrayList<>();
    private long circleCounter = 1;

    public Message(String content, int initialNodeIndex) {
        this.content = content;
        this.initialNodeIndex = initialNodeIndex;
    }

    public int getInitialNodeIndex() {
        return initialNodeIndex;
    }

    public void addTimestamp(Long timestamp) {
        fullCircleTimestamps.add(timestamp);
    }

    public long getCircleCounter() {
        return circleCounter;
    }

    public void incrementCircleCounter() {
        circleCounter++;
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
                ", initialNodeIndex=" + initialNodeIndex +
                ", circleCounter=" + circleCounter +
                '}';
    }
}
