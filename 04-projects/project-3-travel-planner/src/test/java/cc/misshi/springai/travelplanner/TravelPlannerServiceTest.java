package cc.misshi.springai.travelplanner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TravelPlannerService 单元测试 — 0 网络, 0 mockito.
 */
class TravelPlannerServiceTest {

    @Test
    void planShouldProduceCompleteItinerary() {
        // 5 个 null ChatClient → mock 路径
        TravelPlannerService service = new TravelPlannerService(null, null, null, null, null);

        var plan = service.plan(new TravelPlannerService.TravelRequest("海滨", 5, 8000));

        assertThat(plan.destinations()).contains("三亚", "厦门", "青岛");
        assertThat(plan.itinerary()).contains("Day 1", "Day 5");
        assertThat(plan.budget()).contains("8000");
        assertThat(plan.booking()).contains("提前 30 天");
        assertThat(plan.finalPlan()).contains("海滨", "5 天", "8000");
    }

    @Test
    void planShouldHandleDifferentThemes() {
        TravelPlannerService service = new TravelPlannerService(null, null, null, null, null);

        var snowPlan = service.plan(new TravelPlannerService.TravelRequest("雪山", 4, 12000));
        assertThat(snowPlan.destinations()).contains("丽江", "北海道", "瑞士");

        var foodPlan = service.plan(new TravelPlannerService.TravelRequest("美食", 3, 5000));
        assertThat(foodPlan.destinations()).contains("成都", "广州", "台北");
    }

    @Test
    void planShouldUseDefaultsForInvalidInput() {
        TravelPlannerService service = new TravelPlannerService(null, null, null, null, null);

        var plan = service.plan(new TravelPlannerService.TravelRequest(null, 0, 0));
        assertThat(plan.itinerary()).contains("Day 1");
        assertThat(plan.budget()).contains("5000");
    }
}
