package cc.misshi.springai.graph;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/graph")
public class GraphController {

    private final Graph documentReviewGraph;

    public GraphController(Graph documentReviewGraph) {
        this.documentReviewGraph = documentReviewGraph;
    }

    @PostMapping("/review")
    public Map<String, Object> review(@RequestBody ReviewRequest req) {
        Map<String, Object> state = new HashMap<>();
        state.put("text", req.text());
        return documentReviewGraph.run(state, 20);
    }

    public record ReviewRequest(String text) {}
}
