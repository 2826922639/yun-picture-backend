package com.hhh.yunpicturebackend.ai;


import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI识图对话服务接口
 */
public interface AiChatWithImageService {

    /**
     * 带图片的流式对话（核心方法）
     * @param userMessage 用户文本消息
     * @param imageUrl 图片URL
     * @return 流式响应
     */
    @SystemMessage(fromResource = "prompt/image-system-prompt.txt")
    Flux<String> chatWithImageStream(@UserMessage String userMessage,@UserMessage ImageContent imageUrl);
    /**
     * 带图片的流式对话（核心方法）
     * @param imageUrl 图片URL
     * @return 流式响应
     */
    @SystemMessage(fromResource = "prompt/image-understand-prompt.txt")
    String chatWithImage(@UserMessage String userMessage,@UserMessage ImageContent imageUrl);
}