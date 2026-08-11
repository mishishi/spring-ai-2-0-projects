package cc.misshi.springai.knowledgehub;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryRouterTest {

    private final QueryRouter router = new QueryRouter();

    @Test
    void shouldRouteToCodeReview() {
        assertThat(router.route("请帮我审查这段代码")).isEqualTo(QueryRouter.Route.CODE_REVIEW);
        assertThat(router.route("review this function")).isEqualTo(QueryRouter.Route.CODE_REVIEW);
    }

    @Test
    void shouldRouteToWeeklyReport() {
        assertThat(router.route("生成本周周报")).isEqualTo(QueryRouter.Route.WEEKLY_REPORT);
        assertThat(router.route("weekly summary")).isEqualTo(QueryRouter.Route.WEEKLY_REPORT);
    }

    @Test
    void shouldRouteToDocQa() {
        assertThat(router.route("什么是 RAG?")).isEqualTo(QueryRouter.Route.DOC_QA);
        assertThat(router.route("怎么报销?")).isEqualTo(QueryRouter.Route.DOC_QA);
    }

    @Test
    void shouldRouteToChitchat() {
        assertThat(router.route("你好")).isEqualTo(QueryRouter.Route.CHITCHAT);
    }

    @Test
    void emptyQueryShouldRouteToUnknown() {
        assertThat(router.route("")).isEqualTo(QueryRouter.Route.UNKNOWN);
        assertThat(router.route(null)).isEqualTo(QueryRouter.Route.UNKNOWN);
    }
}
