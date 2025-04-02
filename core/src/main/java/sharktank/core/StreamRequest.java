package sharktank.core;

public class StreamRequest {
    private String videoId;
    private double startPosition;
    private double endPosition;
    private String clientCallback;
    private long timestamp;

    public StreamRequest() {
        this.timestamp = System.currentTimeMillis();
    }

    public StreamRequest(String videoId, double startPosition, double endPosition, String clientCallback) {
        this();
        this.videoId = videoId;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.clientCallback = clientCallback;
    }

    // Getters and setters
    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }
    public double getStartPosition() { return startPosition; }
    public void setStartPosition(double startPosition) { this.startPosition = startPosition; }
    public double getEndPosition() { return endPosition; }
    public void setEndPosition(double endPosition) { this.endPosition = endPosition; }
    public String getClientCallback() { return clientCallback; }
    public void setClientCallback(String clientCallback) { this.clientCallback = clientCallback; }
    public long getTimestamp() { return timestamp; }
}