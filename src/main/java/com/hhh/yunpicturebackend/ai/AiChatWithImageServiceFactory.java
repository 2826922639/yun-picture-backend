package com.hhh.yunpicturebackend.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hhh.yunpicturebackend.ai.enums.CodeGenTypeEnum;
import com.hhh.yunpicturebackend.ai.guardrail.PromptSafetyInputGuardrail;
import com.hhh.yunpicturebackend.ai.tools.ToolManager;
import com.hhh.yunpicturebackend.exception.BusinessException;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.service.ChatHistoryService;
import com.hhh.yunpicturebackend.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 代码生成服务工厂
 */
@Configuration
@Slf4j
public class AiChatWithImageServiceFactory {
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;
    /**
     * AI 服务实例缓存
     */
    private final Cache<String, AiChatWithImageService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 pictureId 和代码生成类型获取服务（带缓存）
     */
    public AiChatWithImageService getAiChatWithImageService(long pictureId) {
        String cacheKey = buildCacheKey(pictureId);
        return serviceCache.get(cacheKey, key -> createAiChatWithImageService(pictureId));
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(long pictureId) {
        return pictureId + "_" + "zgwhhh";
    }

    /**
     * 创建新的 AI 识图服务实例
     */
    private AiChatWithImageService createAiChatWithImageService(long pictureId) {
        // 根据 pictureId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(pictureId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(pictureId, chatMemory, 20);
        StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelImage", StreamingChatModel.class);
        return AiServices.builder(AiChatWithImageService.class)
                .streamingChatModel(openAiStreamingChatModel)
                .chatMemory(chatMemory)
                .inputGuardrails(new PromptSafetyInputGuardrail())// 添加输入护轨
                //.outputGuardrails(new RetryOutputGuardrail())  // 添加输出护轨(为了流式输出不使用)
                .build();
    }
}
