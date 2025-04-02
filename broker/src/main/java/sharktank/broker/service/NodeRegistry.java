package sharktank.broker.service;

import org.springframework.stereotype.Component;
import sharktank.core.NodeRegistration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NodeRegistry {
    private final Map<String, NodeRegistration> nodes = new ConcurrentHashMap<>();
    private static final long NODE_TIMEOUT = 30000; // 30 seconds

    public void registerNode(NodeRegistration node) {
        nodes.put(node.getNodeId(), node);
    }

    public void updateHeartbeat(String nodeId) {
        NodeRegistration node = nodes.get(nodeId);
        if (node != null) {
            node.updateHeartbeat();
        }
    }

    public List<NodeRegistration> getActiveNodes() {
        long now = System.currentTimeMillis();
        return nodes.values().stream()
                .filter(node -> (now - node.getLastHeartbeat()) < NODE_TIMEOUT)
                .sorted(Comparator.comparing(NodeRegistration::getNodeId))
                .toList();
    }

    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
    }
}