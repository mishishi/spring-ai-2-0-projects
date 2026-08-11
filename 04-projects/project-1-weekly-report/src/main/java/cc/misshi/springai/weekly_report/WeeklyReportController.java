package cc.misshi.springai.weekly_report;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/report")
public class WeeklyReportController {

    private final WeeklyReportService service;

    public WeeklyReportController(WeeklyReportService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public String generate(@RequestBody ReportRequest req) {
        return service.generate(req.completed(), req.planned(), req.blockers());
    }

    public record ReportRequest(List<String> completed, List<String> planned, List<String> blockers) {}
}
