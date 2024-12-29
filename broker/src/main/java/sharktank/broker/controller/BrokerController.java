package sharktank.broker.controller;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sharktank.core.NodeAssignment;
import sharktank.core.NodeRegistration;
import sharktank.core.VideoRequest;
import sharktank.broker.service.NodeRegistry;


import java.util.*;

@RestController
@CrossOrigin
public class BrokerController {
    private final NodeRegistry nodeRegistry;

    public BrokerController(NodeRegistry nodeRegistry) {
        this.nodeRegistry = nodeRegistry;
    }

    @PostMapping("/broker/register")
    public ResponseEntity<?> registerNode(@RequestBody NodeRegistration registration) {
        String nodeId = UUID.randomUUID().toString();
        registration.setNodeId(nodeId);
        nodeRegistry.registerNode(registration);
        return ResponseEntity.ok().body(Map.of("nodeId", nodeId));
    }

    @PostMapping("/broker/heartbeat/{nodeId}")
    public ResponseEntity<?> heartbeat(@PathVariable String nodeId) {
        nodeRegistry.updateHeartbeat(nodeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/broker/get-nodes")
    public ResponseEntity<?> getNodesForVideo(@RequestBody VideoRequest request) {
        List<NodeRegistration> nodes = nodeRegistry.getActiveNodes();
        if (nodes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("No nodes available");
        }

        List<NodeAssignment> assignments = calculateNodeAssignments(nodes);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/videos")
    public ResponseEntity<?> getAvailableVideos() {
        List<String> allVideos = nodeRegistry.getActiveNodes().stream()
                .filter(node -> node.getAvailableVideos() != null)
                .flatMap(node -> node.getAvailableVideos().stream())
                .distinct() // Removes duplicates
                .sorted() // Optional: Sorts the list alphabetically
                .toList();

        return ResponseEntity.ok(allVideos);
    }

    private List<NodeAssignment> calculateNodeAssignments(List<NodeRegistration> nodes) {
        List<NodeAssignment> assignments = new ArrayList<>();
        int nodeCount = nodes.size();
        double chunkSize = 1.0 / nodeCount;

        for (int i = 0; i < nodeCount; i++) {
            assignments.add(new NodeAssignment(
                    nodes.get(i).getNodeUrl(),
                    i * chunkSize,
                    (i + 1) * chunkSize,
                    i,
                    nodeCount
            ));
        }

        return assignments;
    }
}
