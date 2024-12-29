package sharktank.node.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import sharktank.core.NodeRegistration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class NodeConfig {
    private static final Logger logger = LoggerFactory.getLogger(NodeConfig.class);

    @Value("${server.port}")
    private String serverPort;

    @Value("${broker.url:http://localhost:8080}")
    private String brokerUrl;

    private static final Path VIDEO_DIR = Paths.get("src/main/resources/videos");

    private final RestTemplate restTemplate;

    public NodeConfig(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void registerWithBroker() {
        String nodeUrl = "http://localhost:" + serverPort;

        // Collect the available videos
        List<String> availableVideos = getAvailableVideos();

        NodeRegistration registration = new NodeRegistration(nodeUrl, null, 100, availableVideos);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    brokerUrl + "/broker/register",
                    registration,
                    Map.class
            );

            String nodeId = ((Map) response.getBody()).get("nodeId").toString();
            logger.info("Successfully registered with broker. Node ID: {}", nodeId);

            startHeartbeat(nodeId);
        } catch (Exception e) {
            logger.error("Failed to register with broker", e);
            throw new RuntimeException("Node registration failed", e);
        }
    }

    private void startHeartbeat(String nodeId) {
        new Thread(() -> {
            while (true) {
                try {
                    restTemplate.postForEntity(
                            brokerUrl + "/broker/heartbeat/" + nodeId,
                            null,
                            Void.class
                    );
                    Thread.sleep(10000);
                } catch (Exception e) {
                    logger.error("Heartbeat failed", e);
                }
            }
        }, "HeartbeatThread").start();
    }

    private List<String> getAvailableVideos() {
        try {
            if (!Files.exists(VIDEO_DIR)) {
                Files.createDirectories(VIDEO_DIR);
            }

            List<String> videos = Files.list(VIDEO_DIR)
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".mp4"))
                    .map(path -> path.getFileName().toString().replace(".mp4", ""))
                    .collect(Collectors.toList());

            // Print the list of videos to the console
            System.out.println("Available videos: " + videos);

            return videos;
        } catch (IOException e) {
            System.err.println("Error reading video directory: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
