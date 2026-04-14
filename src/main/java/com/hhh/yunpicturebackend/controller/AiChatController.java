package com.hhh.yunpicturebackend.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hhh.yunpicturebackend.ai.core.AiChatFacade;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.exception.ThrowUtils;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.ratelimter.annotation.RateLimit;
import com.hhh.yunpicturebackend.ratelimter.enums.RateLimitType;
import com.hhh.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @Description ai聊天接口
 **/
@RestController
@RequestMapping("/ai")
public class AiChatController {
    @Resource
    private UserService userService;
    @Resource
    private AiChatFacade aiChatFacade;
    @GetMapping(value ="/chat/image",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI 对话请求过于频繁，请稍后再试")
    public Flux<ServerSentEvent<String>> chatImage(@RequestParam Long pictureId, @RequestParam String message, HttpServletRequest request) {
        //参数校验
        ThrowUtils.throwIf(pictureId == null || pictureId < 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "message 不能为空");
        //获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        //调用服务生成代码（SSE流式返回）
        Flux<String> contentFlux = aiChatFacade.generateUrlStream(message,pictureId,loginUser);
        return contentFlux
                // 过滤掉null值以防止NPE
                .filter(chunk -> chunk != null)
                .map(chunk -> {
                    // 将内容报装成JSON对象
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        // 发送结束事件
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }
    @GetMapping(value ="/chat/rag",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI 对话请求过于频繁，请稍后再试")
    public Flux<ServerSentEvent<String>> chatRAG(@RequestParam String message,@RequestParam Long chatId, HttpServletRequest request) {
        //参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "message 不能为空");
        //获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        //调用服务生成代码（SSE流式返回）
        Flux<String> contentFlux = aiChatFacade.generateStream(message,chatId, loginUser);
        return contentFlux
                // 过滤掉null值以防止NPE
                .filter(chunk -> chunk != null)
                .map(chunk -> {
                    // 将内容报装成JSON对象
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        // 发送结束事件
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }
    @GetMapping(value ="/chat/searchImage",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI 对话请求过于频繁，请稍后再试")
    public Flux<ServerSentEvent<String>> chatSearchImage(@RequestParam String message,@RequestParam Long chatId, HttpServletRequest request) {
        //参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "message 不能为空");
        //获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        //调用服务生成代码（SSE流式返回）
        Flux<String> contentFlux = aiChatFacade.moodSearchImageStream(message,chatId, loginUser);
        return contentFlux
                // 过滤掉null值以防止NPE
                .filter(chunk -> chunk != null)
                .map(chunk -> {
                    // 将内容报装成JSON对象
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        // 发送结束事件
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }
}
