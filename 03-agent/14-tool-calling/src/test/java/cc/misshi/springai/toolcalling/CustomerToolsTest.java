package cc.misshi.springai.toolcalling;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomerTools 单元测试 — 验证 5 大特性.
 */
class CustomerToolsTest {

    private final CustomerTools tools = new CustomerTools();

    @Test
    void getCustomerInfoShouldReturnCustomer() {
        var customer = tools.getCustomerInfo(42L);
        assertThat(customer.id()).isEqualTo(42L);
        assertThat(customer.name()).contains("42");
    }

    @Test
    void updateCustomerInfoWithoutEmailShouldNotChangeEmail() {
        var result = tools.updateCustomerInfo(1L, "Alice", null);
        assertThat(result).contains("Alice").contains("邮箱未修改");
    }

    @Test
    void updateCustomerInfoWithEmailShouldUpdateBoth() {
        var result = tools.updateCustomerInfo(1L, "Alice", "alice@example.com");
        assertThat(result).contains("Alice").contains("alice@example.com");
    }

    @Test
    void listMyCustomersShouldUseToolContext() {
        var ctx = new ToolContext(Map.of("tenantId", "acme-corp"));
        var customers = tools.listMyCustomers(ctx);
        assertThat(customers).hasSize(2);
        assertThat(customers.get(0).name()).contains("acme-corp");
        assertThat(customers.get(1).name()).contains("acme-corp");
    }

    @Test
    void createOrderShouldReturnSummary() {
        var req = new CustomerTools.CreateOrderRequest(
                1L,
                List.of(new CustomerTools.OrderItem("SKU-001", 2, 99.0)),
                198.0
        );
        var result = tools.createOrder(req);
        assertThat(result).contains("客户=1").contains("商品数=1").contains("198.00");
    }

    @Test
    void getAccountBalanceShouldReturnMap() {
        var balance = tools.getAccountBalance(7L);
        assertThat(balance).containsKey("customerId").containsKey("balance");
        assertThat(balance.get("customerId")).isEqualTo(7L);
        assertThat(balance.get("currency")).isEqualTo("CNY");
    }
}
