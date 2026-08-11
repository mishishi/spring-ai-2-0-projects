package cc.misshi.springai.structuredoutput;

/**
 * Demo 1 用的 POJO:人物信息。
 *
 * <p>Spring AI 通过 Jackson 序列化/反序列化 LLM 输出。
 * - 字段必须有 getter / setter(Jackson 用)
 * - 可以用 record(Java 16+)
 */
public record Person(String name, int age, String occupation, String hobby) {
}
