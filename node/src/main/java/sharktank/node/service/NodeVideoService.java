package sharktank.node.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import sharktank.core.StreamRequest;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;

@Service
public class NodeVideoService {
    private static final Logger logger = LoggerFactory.getLogger(NodeVideoService.class);
    private final RestTemplate restTemplate;
    private static final String VIDEO_PATH = "src/main/resources/videos/";
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks

    // Random throttling parameters
    private final int baseThrottleMs;
    private final Random random = new Random();
    private static final int MIN_THROTTLE = 200;  // Minimum 200ms delay
    private static final int MAX_THROTTLE = 1500; // Maximum 1.5s delay
    private static final double VARIATION_FACTOR = 0.3; // 30% variation per chunk

    public NodeVideoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        // Assign a random base throttle time to this node
        this.baseThrottleMs = MIN_THROTTLE + random.nextInt(MAX_THROTTLE - MIN_THROTTLE);
        logger.info("Node initialized with base throttle of {}ms", baseThrottleMs);

        File videoDir = new File(VIDEO_PATH);
        boolean created = videoDir.mkdirs();
        logger.info("Video directory: {} (Created: {})", videoDir.getAbsolutePath(), created);
    }

    private long getRandomizedDelay() {
        // Add random variation of ±30% to the base throttle
        double variation = 1.0 + (random.nextDouble() * VARIATION_FACTOR * 2 - VARIATION_FACTOR);
        return Math.round(baseThrottleMs * variation);
    }

    public void streamVideoChunk(StreamRequest request) {
        try {
            String videoPath = VIDEO_PATH + request.getVideoId() + ".mp4";
            File videoFile = new File(videoPath);

            byte[] fullVideo = Files.readAllBytes(videoFile.toPath());
            int totalSize = fullVideo.length;

            // Calculate this node's portion
            int startByte = (int)(totalSize * request.getStartPosition());
            int endByte = (int)(totalSize * request.getEndPosition());
            int portionSize = endByte - startByte;

            // Calculate chunk information
            int numChunks = (int) Math.ceil((double) portionSize / CHUNK_SIZE);

            logger.info("Node processing portion {}-{} ({} bytes) in {} chunks with base throttle {}ms",
                    startByte, endByte, portionSize, numChunks, baseThrottleMs);

            // Send chunks
            for (int i = 0; i < numChunks; i++) {
                int chunkStart = startByte + (i * CHUNK_SIZE);
                int chunkSize = Math.min(CHUNK_SIZE, endByte - chunkStart);
                byte[] chunk = new byte[chunkSize];
                System.arraycopy(fullVideo, chunkStart, chunk, 0, chunkSize);

                // Calculate position between 0-1 for this chunk
                double chunkPosition = (double) chunkStart / totalSize;
                int positionValue = (int)(chunkPosition * 100);

                String callbackUrl = UriComponentsBuilder
                        .fromUriString(request.getClientCallback())
                        .queryParam("videoId", request.getVideoId())
                        .queryParam("position", positionValue)
                        .queryParam("total", 100)
                        .queryParam("startByte", chunkStart)
                        .queryParam("fileSize", totalSize)
                        .build()
                        .toUriString();

                // Get randomized delay for this chunk
                long chunkDelay = getRandomizedDelay();

                logger.info("Sending chunk {}/{} - Size: {} bytes, Position: {}%, Delay: {}ms",
                        i + 1, numChunks, chunkSize, positionValue, chunkDelay);

                restTemplate.postForEntity(callbackUrl, chunk, Void.class);
                Thread.sleep(chunkDelay);
            }

            logger.info("Completed sending all chunks for portion {}-{}", startByte, endByte);

        } catch (Exception e) {
            logger.error("Error processing video chunk", e);
            throw new RuntimeException("Failed to process video chunk", e);
        }
    }
}