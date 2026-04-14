package com.hhh.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hhh.yunpicturebackend.ai.AiCodeGenTypeRoutingService;
import com.hhh.yunpicturebackend.ai.AiCodeGenTypeRoutingServiceFactory;
import com.hhh.yunpicturebackend.ai.core.AiCodeGeneratorFacade;
import com.hhh.yunpicturebackend.ai.core.builder.VueProjectBuilder;
import com.hhh.yunpicturebackend.ai.core.handler.StreamHandlerExecutor;
import com.hhh.yunpicturebackend.ai.enums.CodeGenTypeEnum;
import com.hhh.yunpicturebackend.constant.AppConstant;
import com.hhh.yunpicturebackend.exception.BusinessException;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.exception.ThrowUtils;
import com.hhh.yunpicturebackend.model.dto.app.AppAddRequest;
import com.hhh.yunpicturebackend.model.dto.app.AppQueryRequest;
import com.hhh.yunpicturebackend.model.entity.App;
import com.hhh.yunpicturebackend.model.entity.ChatHistory;
import com.hhh.yunpicturebackend.model.entity.Space;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.model.enums.ChatHistoryMessageTypeEnum;
import com.hhh.yunpicturebackend.model.vo.AppVO;
import com.hhh.yunpicturebackend.model.vo.UserVO;
import com.hhh.yunpicturebackend.monitor.MonitorContext;
import com.hhh.yunpicturebackend.monitor.MonitorContextHolder;
import com.hhh.yunpicturebackend.service.AppService;
import com.hhh.yunpicturebackend.mapper.AppMapper;
import com.hhh.yunpicturebackend.service.ChatHistoryService;
import com.hhh.yunpicturebackend.service.ScreenshotService;
import com.hhh.yunpicturebackend.service.SpaceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  服务层实现。
 *
 * @author <a href="https://www.zgwyu.online/">嘿嘿嘿</a>
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{
    @Resource
    private UserServiceImpl userService;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
    @Resource
    private VueProjectBuilder vueProjectBuilder;
    @Resource
    private ScreenshotService screenshotService;
    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;
    @Resource
    private SpaceService spaceService;
    /**
     * 通过对话生成代码
     */
    @Override
    public Flux<String> cahttoGenCode(Long appId, String message,List<String> imageUrls, User loginUser) {
        //检查生成次数
        QueryWrapper<Space> SpaceQueryWrapper = new QueryWrapper<>();
        SpaceQueryWrapper.eq("userId", loginUser.getId());
        SpaceQueryWrapper.eq("spaceType", 1);
        Space space = spaceService.getOne(SpaceQueryWrapper);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "请先创建团队空间获取权限！");
        int MaxCount=0;
        if (space.getSpaceLevel()==0){
            MaxCount=5;
        }
        else if (space.getSpaceLevel()==1){
            MaxCount=9;
        }
        else if (space.getSpaceLevel()==2){
            MaxCount=100;
        }
        QueryWrapper<ChatHistory> AppqueryWrapper = new QueryWrapper<>();
        AppqueryWrapper.eq("appId", appId);
        AppqueryWrapper.eq("userId", loginUser.getId());
        AppqueryWrapper.eq("messageType", ChatHistoryMessageTypeEnum.USER.getValue());
        long count = chatHistoryService.count(AppqueryWrapper);
        //ThrowUtils.throwIf(count>MaxCount && !userService.isAdmin(loginUser), ErrorCode.OPERATION_ERROR, "已超出应用修改次数！升级空间可获得更多修改次数！");
        if (count>MaxCount && !userService.isAdmin(loginUser)){
            return Flux.error(new BusinessException(ErrorCode.OPERATION_ERROR, "已超出应用修改次数！升级空间可获得更多修改次数！"));
        }
        //1.参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "请输入内容");
        //2.查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        //3.权限校验（仅本人可以和自己的应用对话）
        ThrowUtils.throwIf(!loginUser.getId().equals(app.getUserId()), ErrorCode.NO_AUTH, "无权限");
        //4.获取应用生成的代码类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        // 5. 通过校验后，添加用户消息到对话历史
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6. 设置监控上下文
        MonitorContextHolder.setContext(
                MonitorContext.builder()
                        .userId(loginUser.getId().toString())
                        .appId(appId.toString())
                        .build()
        );
        // 7. 调用 AI 生成代码（流式）
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message,imageUrls, codeGenTypeEnum, appId);
        // 8. 收集 AI 响应内容并在完成后记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum)
                .doFinally(signalType -> {
                    // 流结束时清理（无论成功/失败/取消）
                    MonitorContextHolder.clearContext();
                });

    }

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        //检查生成次数
        QueryWrapper<Space> SpaceQueryWrapper = new QueryWrapper<>();
        SpaceQueryWrapper.eq("userId", loginUser.getId());
        SpaceQueryWrapper.eq("spaceType", 1);
        Space space = spaceService.getOne(SpaceQueryWrapper);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "请先创建团队空间获取权限！");
        int MaxCount=0;
        if (space.getSpaceLevel()==0){
            MaxCount=5;
        }
        else if (space.getSpaceLevel()==1){
            MaxCount=10;
        }
        else if (space.getSpaceLevel()==2){
            MaxCount=100;
        }
        QueryWrapper<App> AppqueryWrapper = new QueryWrapper<>();
        AppqueryWrapper.eq("userId", loginUser.getId());
        long count = this.count(AppqueryWrapper);
        ThrowUtils.throwIf(count>MaxCount, ErrorCode.OPERATION_ERROR, "已超出应用生成次数！升级空间可获得更多生成次数！");
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型(多例模式)
        AiCodeGenTypeRoutingService routingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum selectedCodeGenType = routingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }

    /**
     * 获取查询包装类
     *
     */
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }
    /**
     * 构造查询对象
     *
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        // 2. 实例化MyBatis-Plus的QueryWrapper（指定实体类泛型）
        QueryWrapper<App> queryWrapper = new QueryWrapper<>();

        // 3. 拼接查询条件：添加非空判断，避免生成无效SQL
        queryWrapper.eq(id != null, "id", id)
                .like(StrUtil.isNotBlank(appName), "appName", appName)
                .like(StrUtil.isNotBlank(cover), "cover", cover)
                .like(StrUtil.isNotBlank(initPrompt), "initPrompt", initPrompt)
                .eq(StrUtil.isNotBlank(codeGenType), "codeGenType", codeGenType)
                .eq(StrUtil.isNotBlank(deployKey), "deployKey", deployKey)
                .eq(priority != null, "priority", priority)
                .eq(userId != null, "userId", userId);

        // 4. 排序逻辑：适配MyBatis-Plus语法，添加非空和白名单校验（生产环境必备）
        // 白名单：限制排序字段只能是实体类的合法字段，防止SQL注入和无效字段
        List<String> validSortFields = List.of("id", "appName", "priority", "userId", "createTime"); // 按需补充
        if (StrUtil.isNotBlank(sortField) && validSortFields.contains(sortField)) {
            boolean isAsc = "ascend".equals(sortOrder);
            // MyBatis-Plus：orderBy(是否升序, 是否忽略null, 字段名)
            queryWrapper.orderBy(isAsc, true, sortField);
        }

        return queryWrapper;
    }
    /**
     * 获取脱敏应用列表
     *
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }
    /**
     * 部署应用
     *
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath, appId);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 将 dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 9. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }
    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新应用封面字段
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }


    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }

}





