package com.hhh.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhh.yunpicturebackend.model.dto.chathistory.ChatHistoryQueryRequest;
import com.hhh.yunpicturebackend.model.entity.ChatHistory;
import com.hhh.yunpicturebackend.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;
import java.util.List;

/**
 *  服务层。
 *
 * @author <a href="https://www.zgwyu.online/">嘿嘿嘿</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {
    /**
     * 保存聊天消息
     *
     * @param appId 应用ID
     * @param message 消息内容
     * @param messageType 消息类型
     * @param userId 用户ID
     * @return 是否添加成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);
    /**
     * 根据应用ID删除所有聊天消息
     *
     * @param appId 应用ID
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);
    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
    /**
     * 分页获取应用对话历史
     *
     * @param appId 应用ID
     * @param pageSize 页面大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser 登录用户
     * @return 应用对话历史列表
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);
    /**
     * 分页获取对话历史（情绪推图）
     *
     * @param chatId ID
     * @param pageSize 页面大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser 登录用户
     * @return 应用对话历史列表
     */
    Page<ChatHistory> listChatHistorySearchImageByPage(Long chatId, int pageSize,
                                            LocalDateTime lastCreateTime,
                                            User loginUser);

    /**
     * 分页获取应用对话历史（图片）
     *
     * @param pictureId 图片ID
     * @param pageSize 页面大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser 登录用户
     * @return 应用对话历史列表
     */
    Page<ChatHistory> listAppChatHistoryWithImageByPage(Long pictureId, int pageSize,
                                                        LocalDateTime lastCreateTime,
                                                        User loginUser);
    /**
     * 分页获取对话历史（RAG）
     *
     * @param chatId ID
     * @param pageSize 页面大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser 登录用户
     * @return 应用对话历史列表
     */
    Page<ChatHistory> listAppChatHistoryWithRAGByPage(Long chatId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser);

    List<Long> getRAGConversationLists(User loginUser);

    List<Long> getImageSearchConversationLists(User loginUser);

    /**
     * 加载应用对话历史到内存
     *
     * @param appId 应用ID
     * @param chatMemory 聊天内存
     * @param maxCount 最大数量
     * @return 加载的聊天记录数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
