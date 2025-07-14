package com.hhh.yunpicturebackend.ai.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.cookie.GlobalCookieManager;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 联网搜索工具
 * 基于百度引擎抓取实时信息
 */
@Slf4j
@Component
public class WebCrawlerSearchTool extends BaseTool {

    private static final String BAIDU_SEARCH_URL = "https://www.baidu.com/s";
    private static final String BAIDU_HOME_URL = "https://www.baidu.com";

    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) Gecko/20100101 Firefox/127.0"
    };

    private static final String[] RESULT_SELECTORS = {
            "div[srcid='1599']", "div[srcid='1902']", "div.c-container", "div.result"
    };

    private static final String[] CONTENT_SELECTORS = {
            "div.c-abstract", "div.abstract", "div.rich-content-inner", "div.answer-content"
    };

    private final Random random = new Random();

    public WebCrawlerSearchTool() {
        // 初始化全局 Cookie 管理器，模拟真实浏览器行为
        GlobalCookieManager.setCookieManager(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
    }

    @Tool("搜索互联网上的实时信息或新闻")
    public String searchBaidu(
            @P("搜索关键词") String query
    ) {
        if (StrUtil.isBlank(query)) {
            return "错误：搜索关键词不能为空";
        }

        log.info("开始执行联网搜索: {}", query);

        try {
            // 1. 模拟首页访问
            preVisitHomePage();

            // 2. 构造请求并添加随机延迟，防止被风控
            TimeUnit.MILLISECONDS.sleep(random.nextInt(1000) + 500);
            String url = BAIDU_SEARCH_URL + "?wd=" + URLEncoder.encode(query, "UTF-8") + "&rn=10";

            HttpResponse response = HttpRequest.get(url)
                    .header("User-Agent", USER_AGENTS[random.nextInt(USER_AGENTS.length)])
                    .header("Referer", BAIDU_HOME_URL)
                    .timeout(15000)
                    .execute();

            if (!response.isOk()) {
                return "搜索失败，搜索引擎响应异常 (Status: " + response.getStatus() + ")";
            }

            String html = response.body();
            if (html.contains("百度安全验证")) {
                return "提示：由于访问频繁，触发了搜索引擎验证，请稍后再试。";
            }

            Document doc = Jsoup.parse(html, CharsetUtil.UTF_8);
            
            // 3. 提取结构化内容
            String highQualityContent = extractHighQualityContent(doc);
            if (StrUtil.isNotBlank(highQualityContent)) {
                return "【优质结果】\n" + highQualityContent;
            }

            String normalResults = extractNormalResults(doc);
            return StrUtil.isNotBlank(normalResults) ? normalResults : "未找到相关的搜索结果。";

        } catch (Exception e) {
            log.error("联网搜索执行异常: ", e);
            return "联网搜索时发生技术错误: " + e.getMessage();
        }
    }

    /**
     * 优先提取百科、问答等高权重卡片内容
     */
    private String extractHighQualityContent(Document doc) {
        // 百度百科
        Element baike = doc.selectFirst("div[srcid='1599'] .lemma-summary, div[srcid='1599'] .abstract");
        if (baike != null) return "[百科] " + cleanText(baike.text());

        // 百度知道/问答
        Element zhidao = doc.selectFirst("div[srcid='1902'] .best-content, div[srcid='1902'] .answer-content");
        if (zhidao != null) return "[问答] " + cleanText(zhidao.text());

        return null;
    }

    /**
     * 提取普通搜索结果列表
     */
    private String extractNormalResults(Document doc) {
        List<String> results = new ArrayList<>();
        int count = 0;

        for (String selector : RESULT_SELECTORS) {
            Elements elements = doc.select(selector);
            for (Element item : elements) {
                if (count >= 5) break; // 仅取前5条结果，防止Token爆炸
                if (item.text().contains("广告")) continue;

                Element titleElem = item.selectFirst("h3");
                if (titleElem == null) continue;

                String title = cleanText(titleElem.text());
                String snippet = "";

                // 尝试寻找摘要
                for (String contentSelector : CONTENT_SELECTORS) {
                    Element contentElem = item.selectFirst(contentSelector);
                    if (contentElem != null) {
                        snippet = cleanText(contentElem.text());
                        break;
                    }
                }

                if (StrUtil.isNotBlank(title)) {
                    results.add(String.format("[%d] %s\n摘要: %s", ++count, title, snippet));
                }
            }
            if (!results.isEmpty()) break;
        }
        return CollUtil.join(results, "\n\n");
    }

    private void preVisitHomePage() {
        try {
            HttpRequest.get(BAIDU_HOME_URL)
                    .header("User-Agent", USER_AGENTS[random.nextInt(USER_AGENTS.length)])
                    .timeout(5000)
                    .execute().close();
        } catch (Exception ignored) {}
    }

    private String cleanText(String text) {
        if (text == null) return "";
        // 限制单条摘要长度，进一步节省 Token
        String cleaned = text.replaceAll("\\s+", " ").trim();
        return StrUtil.maxLength(cleaned, 200);
    }

    @Override
    public String getToolName() {
        return "searchBaidu";
    }

    @Override
    public String getDisplayName() {
        return "联网搜索";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String query = arguments.getStr("query");
        return String.format("[工具调用] %s 关键词: %s", getDisplayName(), query);
    }
}