package com.hhh.yunpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hhh.yunpicturebackend.constant.UserConstant;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.exception.ThrowUtils;
import com.hhh.yunpicturebackend.model.dto.chathistory.ChatHistoryQueryRequest;
import com.hhh.yunpicturebackend.model.entity.App;
import com.hhh.yunpicturebackend.model.entity.ChatHistory;
import com.hhh.yunpicturebackend.model.entity.Picture;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.model.enums.ChatHistoryMessageTypeEnum;
import com.hhh.yunpicturebackend.service.AppService;
import com.hhh.yunpicturebackend.service.ChatHistoryService;
import com.hhh.yunpicturebackend.mapper.ChatHistoryMapper;
import com.hhh.yunpicturebackend.service.PictureService;
import com.hhh.yunpicturebackend.service.UserService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 *  服务层实现。
 *
 * @author <a href="https://www.zgwyu.online/">嘿嘿嘿</a>
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{
    @Resource
    @Lazy
    private AppService appService;
    @Resource
    @Lazy
    private PictureService pictureService;
    @Autowired
    private UserService userService;

    /**
     * 保存聊天消息
     *
     * @param appId 应用ID
     * @param message 消息内容
     * @param messageType 消息类型
     * @param userId 用户ID
     * @return 是否添加成功
     */
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型: " + messageType);
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }
    /**
     * 根据应用ID删除所有聊天消息
     *
     * @param appId 应用ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "ID不能为空");
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("appId", appId);
        return this.remove(queryWrapper);
    }
    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<ChatHistory> getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        // 1. 实例化MyBatis-Plus的QueryWrapper（指定泛型）
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }

        // 2. 获取请求参数
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();

        // 3. 拼接查询条件（注意：MyBatis-Plus的条件方法会自动忽略null值，但原逻辑没做非空判断，这里先保留原逻辑，后续补充优化）
        queryWrapper.eq(id != null, "id", id) // 推荐添加非空判断，避免null值导致的SQL错误
                .like(StrUtil.isNotBlank(message), "message", message) // 模糊查询添加非空判断
                .eq(StrUtil.isNotBlank(messageType), "messageType", messageType) // 非空判断
                .eq(appId != null, "appId", appId) // 非空判断
                .eq(userId != null, "userId", userId); // 非空判断

        // 4. 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }

        // 5. 排序（适配MyBatis-Plus的orderBy语法）
        if (StrUtil.isNotBlank(sortField)) {
            // MyBatis-Plus的orderBy：参数1=是否升序，参数2=是否忽略null，参数3=列名
            queryWrapper.orderBy("ascend".equals(sortOrder), true, sortField);
        } else {
            // 默认按创建时间降序排列（等价于orderByDesc）
            queryWrapper.orderByDesc("createTime");
        }

        return queryWrapper;
    }
    /**
     * 分页获取应用对话历史（应用）
     *
     * @param appId 应用ID
     * @param pageSize 页面大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser 登录用户
     * @return 应用对话历史列表
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }
    /**
     * 分页获取对话历史(情绪荐图)
     *
     * @param pageSize 页面大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser 登录用户
     * @return 应用对话历史列表
     */
    @Override
    public Page<ChatHistory> listChatHistorySearchImageByPage(Long chatId, int pageSize,
                                                   LocalDateTime lastCreateTime,
                                                   User loginUser) {
        ThrowUtils.throwIf(chatId == null || chatId <= 0, ErrorCode.PARAMS_ERROR, "ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN);
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(chatId);
        queryRequest.setUserId(loginUser.getId());
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }
    /**
     * 分页获取对话历史（图片）
     *
     * @param pictureId 图片ID
     * @param pageSize 页面大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser 登录用户
     * @return 应用对话历史列表
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryWithImageByPage(Long pictureId, int pageSize,
                                                               LocalDateTime lastCreateTime,
                                                               User loginUser) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN);
        // 验证权限：只有应用创建者和管理员可以查看
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = picture.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(pictureId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }
    /**
     * 分页获取对话历史（RAG）
     *
     * @param pageSize 页面大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser 登录用户
     * @return 应用对话历史列表
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryWithRAGByPage(Long chatId, int pageSize,
                                                             LocalDateTime lastCreateTime,
                                                             User loginUser) {
        ThrowUtils.throwIf(chatId == null || chatId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN);
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(chatId);
        queryRequest.setUserId(loginUser.getId());
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }
    /**
     * 获取RAG会话列表
     *
     * @param loginUser 登录用户
     * @return RAG会话列表
     */
    @Override
    public List<Long> getRAGConversationLists(User loginUser){
        Long userId = loginUser.getId();
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<ChatHistory>()
                .eq("userId", userId)
                .eq("messageType", "userRAG");
        return this.list(queryWrapper).stream()
                .map(ChatHistory::getAppId)
                .distinct()
                .toList();
    }
    /**
     * 获取图片搜搜会话列表
     *
     * @param loginUser 登录用户
     * @return 图片搜搜会话列表
     */
    @Override
    public List<Long> getImageSearchConversationLists(User loginUser){
        Long userId = loginUser.getId();
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<ChatHistory>()
                .eq("userId", userId)
                .eq("messageType", "userSearchImage");
        return this.list(queryWrapper).stream()
                .map(ChatHistory::getAppId)
                .distinct()
                .toList();
    }

    /**
     * 加载对话历史到内存
     *
     * @param Id ID
     * @param chatMemory 聊天内存
     * @param maxCount 最大数量
     * @return 加载的聊天记录数量
     */
    @Override
    public int loadChatHistoryToMemory(Long Id, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<ChatHistory>()
                    .eq("appId", Id)
                    .orderByDesc("createTime")
                    .last("LIMIT 1, " + maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    // 为 AI 消息添加空的 reasoning_content 字段，兼容 DeepSeek API 要求
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", Id, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", Id, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }

}





