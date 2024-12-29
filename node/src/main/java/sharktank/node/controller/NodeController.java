package sharktank.node.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sharktank.core.StreamRequest;
import sharktank.node.service.NodeVideoService;

@RestController
@CrossOrigin
public class NodeController {
    private final NodeVideoService videoService;

    public NodeController(NodeVideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/node/stream")
    public ResponseEntity<?> streamVideo(@RequestBody StreamRequest request) {
        videoService.streamVideoChunk(request);
        return ResponseEntity.accepted().build();
    }
}