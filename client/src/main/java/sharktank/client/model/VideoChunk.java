// In client project: src/main/java/sharktank/client/model/VideoChunk.java
package sharktank.client.model;

public class VideoChunk {
    private String videoId;
    private long position;
    private byte[] data;

    public VideoChunk() {}

    public VideoChunk(String videoId, long position, byte[] data) {
        this.videoId = videoId;
        this.position = position;
        this.data = data;
    }

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }

    public long getPosition() { return position; }
    public void setPosition(long position) { this.position = position; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}