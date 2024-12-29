package sharktank.core;

import java.io.Serializable;

public class NodeAssignment implements Serializable {
    private String nodeUrl;
    private double startPosition;
    private double endPosition;
    private int position;
    private int total;

    public NodeAssignment() {}

    public NodeAssignment(String nodeUrl, double startPosition, double endPosition, int position, int total) {
        this.nodeUrl = nodeUrl;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.position = position;
        this.total = total;
    }

    // Getters and setters
    public String getNodeUrl() { return nodeUrl; }
    public void setNodeUrl(String nodeUrl) { this.nodeUrl = nodeUrl; }

    public double getStartPosition() { return startPosition; }
    public void setStartPosition(double startPosition) { this.startPosition = startPosition; }

    public double getEndPosition() { return endPosition; }
    public void setEndPosition(double endPosition) { this.endPosition = endPosition; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}
