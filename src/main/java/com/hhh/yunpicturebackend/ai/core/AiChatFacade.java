package com.hhh.yunpicturebackend.ai.core;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hhh.yunpicturebackend.ai.*;
import com.hhh.yunpicturebackend.model.entity.Picture;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.model.enums.ChatHistoryMessageTypeEnum;
import com.hhh.yunpicturebackend.service.ChatHistoryService;
import com.hhh.yunpicturebackend.service.PictureService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import dev.langchain4j.data.message.ImageContent;
// ... 其他引用

@Service
@Slf4j
public class AiChatFacade {

    @Resource
    private AiChatWithImageServiceFactory aiChatWithImageServiceFactory;
    @Resource
    private AiChatWithRAGServiceFactory aiChatWithRAGServiceFactory;
    @Resource
    private AiChatMoodSearchImageServiceFactory aiChatMoodSearchImageServiceFactory;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private PictureService pictureService;

    public Flux<String> generateUrlStream(String userMessage, Long pictureId, User loginUser) {
        // 1. 获取服务实例
        AiChatWithImageService aiChatWithImageService = aiChatWithImageServiceFactory.getAiChatWithImageService(pictureId);

        // 2. 将 URL 转换为 ImageContent 对象（这是关键！）
        String imageUrl = pictureService.getOriginalPicture(pictureId, loginUser);
        ImageContent imageContent = ImageContent.from(imageUrl);

        // 3. 调用修改后的接口方法
        Flux<String> stream = aiChatWithImageService.chatWithImageStream(userMessage, imageContent);

        return processCodeStream(stream,pictureId,loginUser);
    }
    public Flux<String> generateStream(String userMessage, Long chatId, User loginUser) {
        //0.先保存用户消息
        chatHistoryService.addChatMessage(chatId, userMessage, ChatHistoryMessageTypeEnum.USER_RAG.getValue(), loginUser.getId());
        // 1. 获取服务实例
        AiChatWithRAGService aiChatWithRAGService = aiChatWithRAGServiceFactory.getAiChatWithRAGService(chatId);
        // 2. 调用修改后的接口方法
        Flux<String> stream = aiChatWithRAGService.answer(userMessage);

        return processCodeStreamRAG(stream,chatId,loginUser);
    }
    public Flux<String> moodSearchImageStream(String userMessage, Long chatId, User loginUser) {
        //0.先保存用户消息
        chatHistoryService.addChatMessage(chatId, userMessage, ChatHistoryMessageTypeEnum.USER_SEARCH_IMAGE.getValue(), loginUser.getId());
        // 1. 获取服务实例
        AiChatMoodSearchImageService aiChatMoodSearchImageService = aiChatMoodSearchImageServiceFactory.getAiChatMoodSearchImageService(chatId);
        // 2. 调用修改后的接口方法
        Flux<String> stream = aiChatMoodSearchImageService.MoodSearchImageStream(userMessage);

        return processCodeStreamSearchImage(stream,chatId,loginUser);
    }
    private Flux<String> processCodeStream(Flux<String> stream, Long id, User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return stream
                .map(chunk -> {
                    // 收集AI响应内容
                    if (chunk != null) {
                        aiResponseBuilder.append(chunk);
                    }
                    return chunk;
                })
                // 添加filter操作符过滤掉null值
                .filter(chunk -> chunk != null)
                .doOnComplete(() -> {
                    // 流式响应完成后，添加AI消息到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                });
    }
    private Flux<String> processCodeStreamRAG(Flux<String> stream, Long id, User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return stream
                .map(chunk -> {
                    // 收集AI响应内容
                    if (chunk != null) {
                        aiResponseBuilder.append(chunk);
                    }
                    return chunk;
                })
                // 添加filter操作符过滤掉null值
                .filter(chunk -> chunk != null)
                .doOnComplete(() -> {
                    // 流式响应完成后，添加AI消息到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatMessage(id, aiResponse, ChatHistoryMessageTypeEnum.AI_RAG.getValue(), loginUser.getId());
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(id, errorMessage, ChatHistoryMessageTypeEnum.AI_RAG.getValue(), loginUser.getId());
                });
    }
    private Flux<String> processCodeStreamSearchImage(Flux<String> stream, Long id, User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return stream
                .map(chunk -> {
                    // 收集AI响应内容
                    if (chunk != null) {
                        aiResponseBuilder.append(chunk);
                    }
                    return chunk;
                })
                // 添加filter操作符过滤掉null值
                .filter(chunk -> chunk != null)
                .doOnComplete(() -> {
                    // 流式响应完成后，添加AI消息到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatMessage(id, aiResponse, ChatHistoryMessageTypeEnum.AI_SEARCH_IMAGE.getValue(), loginUser.getId());
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(id, errorMessage, ChatHistoryMessageTypeEnum.USER_SEARCH_IMAGE.getValue(), loginUser.getId());
                });
    }
}
