package sharktank.client.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import sharktank.core.StreamRequest;
import sharktank.core.VideoRequest;
import sharktank.core.NodeAssignment;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ClientVideoService {
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;

    @Autowired
    public ClientVideoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.executorService = Executors.newCachedThreadPool();
    }

    public void requestVideoFromNodes(List<NodeAssignment> assignments, VideoRequest request) {
        CompletableFuture<Void>[] futures = new CompletableFuture[assignments.size()];

        for (int i = 0; i < assignments.size(); i++) {
            NodeAssignment assignment = assignments.get(i);

            StreamRequest streamRequest = new StreamRequest(
                    request.getVideoId(),
                    assignment.getStartPosition(),
                    assignment.getEndPosition(),
                    request.getClientCallback() + "?total=" + assignment.getTotal() +
                            "&position=" + assignment.getPosition()
            );

            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    restTemplate.postForEntity(
                            assignment.getNodeUrl() + "/node/stream",
                            streamRequest,
                            Void.class
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Failed to send request to node: " + assignment.getNodeUrl(), e);
                }
            }, executorService);
        }

        CompletableFuture.allOf(futures).join();
    }
}