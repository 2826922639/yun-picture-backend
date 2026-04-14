package com.hhh.yunpicturebackend.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhh.yunpicturebackend.model.dto.app.AppAddRequest;
import com.hhh.yunpicturebackend.model.dto.app.AppQueryRequest;
import com.hhh.yunpicturebackend.model.entity.App;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
* @author shoto
* @description 针对表【app(应用)】的数据库操作Service
* @createDate 2025-12-17 11:29:05
*/
public interface AppService extends IService<App> {
    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 获取查询包装类
     *
     */
    AppVO getAppVO(App app);
    /**
     * 构造查询对象
     *
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);
    /**
     * 获取脱敏应用列表
     *
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 通过对话生成代码
     */
    Flux<String> cahttoGenCode(Long appId, String message,List<String> imageUrls, User loginUser);
    /**
     * 部署应用
     */
    String deployApp(Long appId, User loginUser);

    void generateAppScreenshotAsync(Long appId, String appUrl);
}
