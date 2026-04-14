package com.hhh.yunpicturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhh.yunpicturebackend.annotation.AuthCheck;
import com.hhh.yunpicturebackend.common.BaseResponse;
import com.hhh.yunpicturebackend.common.ResultUtils;
import com.hhh.yunpicturebackend.constant.UserConstant;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.exception.ThrowUtils;
import com.hhh.yunpicturebackend.model.dto.chathistory.ChatHistoryQueryRequest;
import com.hhh.yunpicturebackend.model.entity.ChatHistory;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.service.ChatHistoryService;
import com.hhh.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 *  控制层。
 *
 * @author <a href="https://www.zgwyu.online/">嘿嘿嘿</a>
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private UserService userService;

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }
    /**
     * 分页查询对话历史图片（游标查询）
     *
     * @param pictureId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/picture/{pictureId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistoryImage(@PathVariable Long pictureId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryWithImageByPage(pictureId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }
    /**
     * 分页查询对话历史RAG（游标查询）
     *
     * @param chatId          ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/RAG/{chatId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistoryRAG(@PathVariable Long chatId,
                                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                                   @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                                   HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryWithRAGByPage(chatId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }
    /**
     * 获取RAG会话列表
     * @return RAG会话列表
     */
    @GetMapping("/RAG")
    public BaseResponse<List<Long>>getRAGConversationLists(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(chatHistoryService.getRAGConversationLists(loginUser));
    }
    /**
     * 获取情绪搜图会话列表
     * @return SearchImage会话列表
     */
    @GetMapping("/SearchImage")
    public BaseResponse<List<Long>>getSearchImageConversationLists(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(chatHistoryService.getImageSearchConversationLists(loginUser));
    }
    /**
     * 分页查询对话历史RAG（游标查询）
     *
     * @param chatId          ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/searchImage/{chatId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistorySearchImage(@PathVariable Long chatId,
                                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                                 @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryWithRAGByPage(chatId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }
    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getCurrent();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 查询数据
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }
    /**
     * 删除对话历史
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChatHistory(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("appId", id);
        queryWrapper.last("limit 1"); // 强制 SQL 加上 limit 1
        ChatHistory chatHistory = chatHistoryService.getOne(queryWrapper);
        ThrowUtils.throwIf(chatHistory == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!chatHistory.getUserId().equals(loginUser.getId()) && !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE), ErrorCode.NO_AUTH);
        boolean result = chatHistoryService.deleteByAppId(chatHistory.getAppId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}
