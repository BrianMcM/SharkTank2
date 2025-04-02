package sharktank.core;

public class VideoRequest {
    private String videoId;
    private String clientCallback;
    private long timestamp;
    private VideoMetadata metadata;

    public VideoRequest() {
        this.timestamp = System.currentTimeMillis();
    }

    public static class VideoMetadata {
        private long fileSize;
        private String format;
        private int quality;

        // Getters and setters
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public int getQuality() { return quality; }
        public void setQuality(int quality) { this.quality = quality; }
    }

    // Getters and setters
    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }
    public String getClientCallback() { return clientCallback; }
    public void setClientCallback(String clientCallback) { this.clientCallback = clientCallback; }
    public long getTimestamp() { return timestamp; }
    public VideoMetadata getMetadata() { return metadata; }
    public void setMetadata(VideoMetadata metadata) { this.metadata = metadata; }
}