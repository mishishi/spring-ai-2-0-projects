package cc.misshi.springai.structuredoutput;

/**
 * Demo 2 用的 POJO:电影信息。
 */
public record Movie(String title, int year, String director, double rating) {
}
