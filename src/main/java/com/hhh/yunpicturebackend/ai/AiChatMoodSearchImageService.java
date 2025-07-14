package com.hhh.yunpicturebackend.ai;


import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * AI情绪推荐图片服务
 */
public interface AiChatMoodSearchImageService {

    /**
     * 根据情绪推荐图片
     * @param userMessage 文本消息
     * @return 流式响应
     */
    @SystemMessage(fromResource = "prompt/mood-system-prompt.txt")
    Flux<String> MoodSearchImageStream(@UserMessage String userMessage);
}