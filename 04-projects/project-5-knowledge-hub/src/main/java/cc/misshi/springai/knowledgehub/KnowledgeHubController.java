package cc.misshi.springai.knowledgehub;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hub")
public class KnowledgeHubController {

    private final KnowledgeHubService service;

    public KnowledgeHubController(KnowledgeHubService service) {
        this.service = service;
    }

    @PostMapping("/ask")
    public KnowledgeHubService.HubResponse ask(@RequestBody KnowledgeHubService.HubRequest req) {
        return service.handle(req);
    }
}
