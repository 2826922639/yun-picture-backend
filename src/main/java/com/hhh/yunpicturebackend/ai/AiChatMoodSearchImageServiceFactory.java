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
import dev.langchain4j.data.message.ToolExecutionResultMessage;
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

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;

/**
 * 代码生成服务工厂
 */
@Configuration
@Slf4j
public class AiChatMoodSearchImageServiceFactory {
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private ToolManager toolManager;
    /**
     * AI 服务实例缓存
     */
    private final Cache<String, AiChatMoodSearchImageService> serviceCache = Caffeine.newBuilder()
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
    public AiChatMoodSearchImageService getAiChatMoodSearchImageService(long chatId) {
        String cacheKey = buildCacheKey(chatId);
        return serviceCache.get(cacheKey, key -> createAiChatMoodSearchImageService(chatId));
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(long chatId) {
        return chatId + "_" + "zgwsearch";
    }

    /**
     * 创建新的 AI RAG服务实例
     */
    private AiChatMoodSearchImageService createAiChatMoodSearchImageService(long chatId) {
        // 根据 userId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(chatId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(chatId, chatMemory, 20);
        StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
        return AiServices.builder(AiChatMoodSearchImageService.class)
                .streamingChatModel(openAiStreamingChatModel)
                .chatMemory(chatMemory)
                .tools(toolManager.getTool("searchPictureByMood"))
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                ))
                .inputGuardrails(new PromptSafetyInputGuardrail())// 添加输入护轨
                //.outputGuardrails(new RetryOutputGuardrail())  // 添加输出护轨(为了流式输出不使用)
                .build();
    }
}
