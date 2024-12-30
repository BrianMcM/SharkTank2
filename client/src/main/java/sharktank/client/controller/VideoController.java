package sharktank.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


@RestController
@CrossOrigin
public class VideoController {
	private static final Logger logger = LoggerFactory.getLogger(VideoController.class);
	private final Map<String, ChunkAssembler> videoAssemblers = new ConcurrentHashMap<>();
	private static final String SAVE_PATH = Paths.get("").toAbsolutePath()
			.resolve("videos/").toString();
	private static final long CHUNK_SIZE = 1024 * 1024; // 1MB chunk size

	private String vidID = "";

	@GetMapping("/video/status")
	public ResponseEntity<?> getVideoStatus(@RequestParam(required = false) String videoId) {
		Map<String, Object> status = new HashMap<>();
		String vid = vidID;
		ChunkAssembler assembler = videoAssemblers.get(vid);

		if (assembler != null) {
			status.put("complete", assembler.isComplete());
			status.put("progress", assembler.getProgress());
			status.put("receivedBytes", assembler.getReceivedBytes());
			status.put("expectedSize", assembler.getExpectedSize());
		} else {
			Path videoPath = Paths.get(SAVE_PATH, vid + ".mp4");
			System.out.println(videoPath);
			if (Files.exists(videoPath)) {
				try {
					long size = Files.size(videoPath);
					status.put("complete", true);
					status.put("progress", 1.0);
					status.put("receivedBytes", size);
					status.put("expectedSize", size);
				} catch (IOException e) {
					logger.error("Error checking video file", e);
					return ResponseEntity.internalServerError().build();
				}
			} else {
				status.put("complete", false);
				status.put("progress", 0.0);
				status.put("receivedBytes", 0);
				status.put("expectedSize", 0);
			}
		}

		return ResponseEntity.ok(status);
	}
		

	@GetMapping("/video/play/{videoId}")
	public ResponseEntity<ResourceRegion> playVideo(
			@PathVariable String videoId,
			@RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {

		Path videoPath = Paths.get(SAVE_PATH, videoId + ".mp4");
		if (!Files.exists(videoPath)) {
			return ResponseEntity.notFound().build();
		}

		UrlResource video = new UrlResource(videoPath.toUri());
		ResourceRegion region;

		try {
			long contentLength = video.contentLength();
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
			headers.add(HttpHeaders.CONTENT_TYPE, "video/mp4");
			headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
			headers.add(HttpHeaders.PRAGMA, "no-cache");
			headers.add(HttpHeaders.EXPIRES, "0");

			if (rangeHeader == null || rangeHeader.trim().isEmpty()) {
				logger.info("Full video request - Size: {} bytes", contentLength);
				region = new ResourceRegion(video, 0, Math.min(CHUNK_SIZE, contentLength));
				headers.add(HttpHeaders.CONTENT_RANGE, "bytes 0-" + (region.getCount() - 1) + "/" + contentLength);
				return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
						.headers(headers)
						.body(region);
			}

			String[] ranges = rangeHeader.replace("bytes=", "").split("-");
			long start = Long.parseLong(ranges[0]);
			long end = ranges.length > 1 && !ranges[1].isEmpty()
					? Long.parseLong(ranges[1])
					: Math.min(start + CHUNK_SIZE, contentLength - 1);

			if (start >= contentLength) {
				headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength);
				return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
						.headers(headers)
						.build();
			}

			region = new ResourceRegion(video, start, Math.min(CHUNK_SIZE, end - start + 1));
			headers.add(HttpHeaders.CONTENT_RANGE,
					String.format("bytes %d-%d/%d", start, start + region.getCount() - 1, contentLength));

			logger.info("Serving bytes {}-{}/{}", start, start + region.getCount() - 1, contentLength);

			return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
					.headers(headers)
					.body(region);

		} catch (IOException e) {
			logger.error("Error streaming video", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/video/chunk")
	public ResponseEntity<?> receiveChunk(
			@RequestBody byte[] chunk,
			@RequestParam String videoId,
			@RequestParam int position,
			@RequestParam int total,
			@RequestParam long startByte,
			@RequestParam long fileSize) {

		logger.info("Received chunk - VideoId: {}, Position: {}/{}, Size: {} bytes",
				videoId, position, total, chunk.length);
		
		vidID = videoId;

		ChunkAssembler assembler = videoAssemblers.computeIfAbsent(videoId,
				id -> new ChunkAssembler(fileSize));

		if (assembler.addChunk(startByte, chunk) && !assembler.isProcessed()) {
			byte[] completeVideo = assembler.assembleVideo();
			if (completeVideo != null) {
				processCompleteVideo(videoId, completeVideo);
				videoAssemblers.remove(videoId);
				logger.info("Video processing completed for: {}", videoId);
			}
		}

		return ResponseEntity.ok().build();
	}
	private void processCompleteVideo(String videoId, byte[] completeVideo) {
		try {
			File saveDir = new File(SAVE_PATH);
			if (!saveDir.exists()) {
				boolean created = saveDir.mkdirs();
				logger.info("Creating directory {}: {}", SAVE_PATH, created);
			}

			String filename = videoId + ".mp4";
			File videoFile = new File(saveDir, filename);

			logger.info("Saving video to: {} (size: {} bytes)",
					videoFile.getAbsolutePath(), completeVideo.length);

			try (FileOutputStream fos = new FileOutputStream(videoFile)) {
				fos.write(completeVideo);
				fos.flush();
			}

			logger.info("Video saved successfully. File size: {} bytes", videoFile.length());

		} catch (IOException e) {
			logger.error("Error saving video file", e);
			throw new RuntimeException("Failed to save video file", e);
		}
	}

	private static class ChunkAssembler {
		private final NavigableMap<Long, byte[]> chunks = new TreeMap<>();
		private final long expectedSize;
		private long receivedBytes = 0;
		private final AtomicBoolean processed = new AtomicBoolean(false);

		public ChunkAssembler(long expectedSize) {
			this.expectedSize = expectedSize;
			logger.info("Created new assembler - Expected size: {} bytes", expectedSize);
		}

		public synchronized boolean addChunk(long position, byte[] chunk) {
			if (!chunks.containsKey(position)) {
				chunks.put(position, chunk.clone());
				receivedBytes += chunk.length;
				logger.info("Added chunk at byte position {}. Total received: {}/{} bytes",
						position, receivedBytes, expectedSize);

				return isComplete();
			}
			logger.warn("Duplicate chunk received for byte position: {}", position);
			return false;
		}

		public boolean isComplete() {
			if (receivedBytes < expectedSize) return false;

			// Verify continuous byte ranges
			long currentPosition = 0;
			for (Map.Entry<Long, byte[]> entry : chunks.entrySet()) {
				if (entry.getKey() != currentPosition) {
					logger.warn("Gap detected in byte ranges at position: {}", currentPosition);
					return false;
				}
				currentPosition += entry.getValue().length;
			}

			logger.info("Video assembly complete - All chunks received and verified");
			return currentPosition == expectedSize;
		}

		public boolean isProcessed() {
			return processed.get();
		}

		public byte[] assembleVideo() {
			if (!isComplete() || !processed.compareAndSet(false, true)) {
				return null;
			}

			byte[] result = new byte[(int)expectedSize];
			int position = 0;

			logger.info("Starting video assembly - {} chunks, {} total bytes",
					chunks.size(), expectedSize);

			for (Map.Entry<Long, byte[]> entry : chunks.entrySet()) {
				byte[] chunk = entry.getValue();
				System.arraycopy(chunk, 0, result, position, chunk.length);
				position += chunk.length;
				logger.info("Assembled chunk at position {} (size: {})",
						entry.getKey(), chunk.length);
			}

			logger.info("Video assembly completed successfully");
			return result;
		}

		public double getProgress() {
			return (double) receivedBytes / expectedSize;
		}

		public long getReceivedBytes() {
			return receivedBytes;
		}

		public long getExpectedSize() {
			return expectedSize;
		}
	}
}