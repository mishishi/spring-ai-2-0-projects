package cc.misshi.springai.travelplanner;

import org.springframework.web.bind.annotation.*;

/**
 * Travel Planner HTTP API.
 */
@RestController
@RequestMapping("/travel")
public class TravelPlannerController {

    private final TravelPlannerService service;

    public TravelPlannerController(TravelPlannerService service) {
        this.service = service;
    }

    @PostMapping("/plan")
    public TravelPlannerService.TravelPlan plan(@RequestBody TravelPlannerService.TravelRequest req) {
        return service.plan(req);
    }
}
