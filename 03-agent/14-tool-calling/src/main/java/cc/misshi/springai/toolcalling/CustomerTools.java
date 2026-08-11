package cc.misshi.springai.toolcalling;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 客户管理工具集 — 第 14 章 5 大特性演示.
 *
 * <p>每个方法演示一个不同点,跟第 13 章 "基础" 区别开来.
 */
@Component
public class CustomerTools {

    /**
     * 演示 1: 必选参数 + returnDirect.
     * <p>returnDirect=true → 工具结果直接返回给调用者,不再让模型加工.
     * 适用场景:工具已经是最终答案(如查询数据库得到的结果).
     */
    @Tool(description = "根据客户 ID 查询完整客户信息,直接返回结构化数据", returnDirect = true)
    public Customer getCustomerInfo(
            @ToolParam(description = "客户 ID,正整数") Long id) {
        return findById(id);
    }

    /**
     * 演示 2: 可选参数 + 默认值.
     * <p>required = false 表示模型可以不传这个参数.
     * 适用场景:有些参数是 optional(如 email 不一定填).
     */
    @Tool(description = "更新客户信息,email 是可选的")
    public String updateCustomerInfo(
            @ToolParam(description = "客户 ID") Long id,
            @ToolParam(description = "新的客户名称") String name,
            @ToolParam(description = "新的邮箱地址(可选,不更新传空字符串)", required = false) String email) {
        if (email == null || email.isBlank()) {
            return String.format("已更新客户 %d 的名称为 '%s'(邮箱未修改)", id, name);
        }
        return String.format("已更新客户 %d: 名称='%s', 邮箱='%s'", id, name, email);
    }

    /**
     * 演示 3: ToolContext 传入租户隔离.
     * <p>工具可以接收 ToolContext,从中拿用户/租户/请求 ID 等元数据.
     * 适用场景:多租户 SaaS,根据 tenantId 路由数据.
     */
    @Tool(description = "查询当前租户的所有客户列表(自动从 ToolContext 拿 tenantId)")
    public List<Customer> listMyCustomers(ToolContext toolContext) {
        String tenantId = (String) toolContext.getContext().get("tenantId");
        // mock 数据:返回 2 个客户
        return List.of(
                new Customer(1L, "Alice (" + tenantId + ")", "alice@" + tenantId + ".com"),
                new Customer(2L, "Bob (" + tenantId + ")", "bob@" + tenantId + ".com")
        );
    }

    /**
     * 演示 4: POJO 复杂参数.
     * <p>Spring AI 会自动从 POJO 生成 JSON Schema,模型可以传嵌套对象.
     */
    @Tool(description = "创建一个新订单,需要客户 ID + 至少一个商品")
    public String createOrder(CreateOrderRequest request) {
        return String.format("订单已创建:客户=%d, 商品数=%d, 总价=%.2f",
                request.customerId(), request.items().size(), request.totalAmount());
    }

    /**
     * 演示 5: 返回 Map<String, Object>.
     * <p>返回灵活结构(非固定 record/class),模型拿到的是 JSON.
     */
    @Tool(description = "查询客户的账户余额,返回余额+币种+最近交易笔数")
    public Map<String, Object> getAccountBalance(
            @ToolParam(description = "客户 ID") Long customerId) {
        return Map.of(
                "customerId", customerId,
                "balance", 1234.56,
                "currency", "CNY",
                "recentTransactions", 5
        );
    }

    // --- mock 数据(0 网络) ---
    private Customer findById(Long id) {
        return new Customer(id, "Customer " + id, "customer" + id + "@example.com");
    }

    // --- 内部 record ---
    public record Customer(Long id, String name, String email) {}
    public record CreateOrderRequest(Long customerId, List<OrderItem> items, double totalAmount) {}
    public record OrderItem(String sku, int quantity, double price) {}
}
