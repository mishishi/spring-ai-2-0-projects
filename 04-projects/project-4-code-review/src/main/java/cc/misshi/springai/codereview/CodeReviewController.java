package cc.misshi.springai.codereview;

import org.springframework.web.bind.annotation.*;

/**
 * Code Review HTTP API.
 */
@RestController
@RequestMapping("/review")
public class CodeReviewController {

    private final CodeReviewService service;

    public CodeReviewController(CodeReviewService service) {
        this.service = service;
    }

    @PostMapping("/code")
    public CodeReviewService.ReviewReport reviewCode(@RequestBody ReviewRequest req) {
        return service.review(req.code(), req.language());
    }

    public record ReviewRequest(String code, String language) {}
}
