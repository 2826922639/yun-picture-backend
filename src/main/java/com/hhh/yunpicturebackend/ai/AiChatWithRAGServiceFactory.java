package com.hhh.yunpicturebackend.ai;

import ai.djl.util.Utils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hhh.yunpicturebackend.ai.guardrail.PromptSafetyInputGuardrail;
import com.hhh.yunpicturebackend.ai.tools.ToolManager;
import com.hhh.yunpicturebackend.service.ChatHistoryService;
import com.hhh.yunpicturebackend.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

/**
 * 代码生成服务工厂
 */
@Configuration
@Slf4j
public class AiChatWithRAGServiceFactory {
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private ToolManager toolManager;
    /**
     * AI 服务实例缓存
     */
    private final Cache<String, AiChatWithRAGService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 userId 和代码生成类型获取服务（带缓存）
     */
    public AiChatWithRAGService getAiChatWithRAGService(long chatId) {
        String cacheKey = buildCacheKey(chatId);
        return serviceCache.get(cacheKey, key -> createAiChatWithRAGService(chatId));
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(long chatId) {
        return chatId + "_" + "zgwuser";
    }

    /**
     * 创建新的 AI RAG服务实例
     */
    private AiChatWithRAGService createAiChatWithRAGService(long chatId) {
        List<Document> documents = loadDocuments(toPath("documents/"), glob("*.txt"));
        // 根据 userId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(chatId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(chatId, chatMemory, 20);
        StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
        return AiServices.builder(AiChatWithRAGService.class)
                .streamingChatModel(openAiStreamingChatModel)
                .chatMemory(chatMemory)
                .contentRetriever(createContentRetriever(documents))
                .inputGuardrails(new PromptSafetyInputGuardrail())// 添加输入护轨
                //.outputGuardrails(new RetryOutputGuardrail())  // 添加输出护轨(为了流式输出不使用)
                .build();
    }
    private static ContentRetriever createContentRetriever(List<Document> documents) {

        // 为文档及其嵌入创建一个空的内存存储。
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 将我们的文档导入到embeddingStore中.
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        // 从嵌入存储中创建一个内容检索器
        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }
    public static PathMatcher glob(String glob) {
        return FileSystems.getDefault().getPathMatcher("glob:" + glob);
    }

    public static Path toPath(String relativePath) {
        try {
            URL fileUrl = Utils.class.getClassLoader().getResource(relativePath);
            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
