package sharktank.core;

import java.io.Serializable;
import java.util.List;

public class NodeRegistration implements Serializable {
    private String nodeUrl;
    private String nodeId;
    private long lastHeartbeat;
    private int capacity;
    private NodeStatus status;
    private List<String> availableVideos; // New field for storing available videos

    public enum NodeStatus {
        ACTIVE, IDLE, OVERLOADED
    }

    public NodeRegistration() {}

    public NodeRegistration(String nodeUrl, String nodeId, int capacity, List<String> availableVideos) {
        this.nodeUrl = nodeUrl;
        this.nodeId = nodeId;
        this.capacity = capacity;
        this.lastHeartbeat = System.currentTimeMillis();
        this.status = NodeStatus.IDLE;
        this.availableVideos = availableVideos;
    }

    public String getNodeUrl() { return nodeUrl; }
    public String getNodeId() { return nodeId; }
    public long getLastHeartbeat() { return lastHeartbeat; }
    public int getCapacity() { return capacity; }
    public NodeStatus getStatus() { return status; }
    public List<String> getAvailableVideos() { return availableVideos; } // Getter for available videos

    public void setNodeUrl(String nodeUrl) { this.nodeUrl = nodeUrl; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setStatus(NodeStatus status) { this.status = status; }
    public void setAvailableVideos(List<String> availableVideos) { this.availableVideos = availableVideos; } // Setter for available videos

    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }
}