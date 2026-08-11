package cc.misshi.springai.streaming;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 简单 HTML 页面,演示打字机效果。
 *
 * <p>前端 EventSource 接 /api/chat/stream,实时显示 LLM 输出。
 */
@Controller
public class PageController {

    /**
     * 返回单文件 HTML,纯 vanilla JS,无依赖。
     */
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String index() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>Spring AI Streaming Demo</title>
                    <style>
                        body { font-family: -apple-system, sans-serif; max-width: 720px; margin: 40px auto; padding: 0 20px; }
                        #output { white-space: pre-wrap; min-height: 200px; border: 1px solid #ddd; padding: 16px; border-radius: 8px; line-height: 1.6; }
                        button { padding: 8px 16px; margin-right: 8px; cursor: pointer; }
                        input { width: 400px; padding: 8px; }
                    </style>
                </head>
                <body>
                    <h1>🤖 Spring AI Streaming Demo</h1>
                    <p>
                        <input id="q" value="用三句话介绍 Spring AI 的核心特性" />
                        <button id="sync">同步</button>
                        <button id="stream">流式</button>
                    </p>
                    <div id="output"></div>
                    <script>
                        const output = document.getElementById('output');
                        const q = document.getElementById('q');

                        document.getElementById('sync').onclick = async () => {
                            output.textContent = '⏳ 同步请求中...';
                            const r = await fetch('/api/chat/sync?q=' + encodeURIComponent(q.value));
                            const text = await r.text();
                            output.textContent = text;
                        };

                        document.getElementById('stream').onclick = () => {
                            output.textContent = '⏳ 流式接收中...';
                            const es = new EventSource('/api/chat/stream?q=' + encodeURIComponent(q.value));
                            es.onmessage = e => {
                                if (output.textContent.startsWith('⏳')) output.textContent = '';
                                output.textContent += e.data;
                            };
                            es.onerror = () => { es.close(); output.textContent += '\\n(流结束)'; };
                        };
                    </script>
                </body>
                </html>
                """;
    }
}
