package cc.misshi.springai.docqa;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qa")
public class DocQaController {

    private final DocQaService service;

    public DocQaController(DocQaService service) {
        this.service = service;
    }

    @PostMapping("/ask")
    public String ask(@RequestBody AskRequest req) {
        return service.ask(req.question());
    }

    public record AskRequest(String question) {}
}
