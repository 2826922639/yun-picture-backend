package com.hhh.yunpicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hhh.yunpicturebackend.annotation.AuthCheck;
import com.hhh.yunpicturebackend.api.aliyunai.AliYunAiApi;
import com.hhh.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.hhh.yunpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.hhh.yunpicturebackend.api.imagesearch.ImagSearchApiFacade;
import com.hhh.yunpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.hhh.yunpicturebackend.common.BaseResponse;
import com.hhh.yunpicturebackend.common.DeleteRequeat;
import com.hhh.yunpicturebackend.common.ResultUtils;
import com.hhh.yunpicturebackend.constant.UserConstant;
import com.hhh.yunpicturebackend.exception.BusinessException;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.exception.ThrowUtils;
import com.hhh.yunpicturebackend.model.dto.pictuer.*;
import com.hhh.yunpicturebackend.model.entity.Picture;
import com.hhh.yunpicturebackend.model.entity.Space;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import com.hhh.yunpicturebackend.model.vo.PictureTagCategory;
import com.hhh.yunpicturebackend.model.vo.PictureVO;
import com.hhh.yunpicturebackend.service.PictureService;
import com.hhh.yunpicturebackend.service.SpaceService;
import com.hhh.yunpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private AliYunAiApi aliYunAiApi;

    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000L)//最大10000条
            .expireAfterWrite(Duration.ofMinutes(5))// 缓存失效时间
            .build();
    @Autowired
    private SpaceService spaceService;

    /**
     * 上传图片(可以更新图片)
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 上传图片(URL)
     *
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequeat deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = pictureService.delete(deleteRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = pictureService.updatePicture(pictureUpdateRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //空间权限校验
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(loginUser, picture);
        }
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            //公开图库
            //普通用户默认只能看到审核通过的数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            //私有图库
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在！");
            if (!loginUser.getId().equals(space.getUserId())) {
                ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH, "没有空间权限！");
            }
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 分页获取图片列表（封装类）（有缓存）
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                                      HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            //公开图库
            //普通用户默认只能看到审核通过的数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            //私有图库
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在！");
            if (!loginUser.getId().equals(space.getUserId())) {
                ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH, "没有空间权限！");
            }
        }

        //查询缓存，缓存不存在则查询数据库
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String redisKey = String.format("yunpicture:listPictureVOByPage:%s", hashKey);
        //1.先查本地缓存
        String cacheValue = LOCAL_CACHE.getIfPresent(redisKey);
        if (StrUtil.isNotBlank(cacheValue)) {
            //如果本地缓存命中直接返回结果
            Page<PictureVO> cachPage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(cachPage);
        }
        //2.本地缓存未命中查询redis分布式缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cacheValue = opsForValue.get(redisKey);
        if (StrUtil.isNotBlank(cacheValue)) {
            //redis分布式缓存命中直接返回结果并且更新本地缓存
            LOCAL_CACHE.put(redisKey, cacheValue);
            Page<PictureVO> cachPage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(cachPage);
        }
        // 3.如果缓存都没命中查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        //4.更新两个缓存
        //更新redis
        String cacheValue0 = JSONUtil.toJsonStr(pictureVOPage);
        // 设置过期时间,5-10分钟，防止缓存雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        stringRedisTemplate.opsForValue().set(redisKey, cacheValue0, cacheExpireTime, TimeUnit.SECONDS);
        //存入本地缓存
        LOCAL_CACHE.put(redisKey, cacheValue0);
        // 获取封装类
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(result);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量抓取并创建图片
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Integer uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 以图搜图接口
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest pictureUploadByBatchRequest,
                                                                        HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = pictureUploadByBatchRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImagSearchApiFacade.searchImage(picture.getUrl());
        return ResultUtils.success(resultList);
    }
    /**
     * 颜色搜索图片
     */
    @PostMapping("/search/color")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest,
                                                              HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        ThrowUtils.throwIf(picColor == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(searchPictureByColorRequest.getSpaceId(), picColor, loginUser);
        return ResultUtils.success(pictureVOList);
    }
    /**
     * 批量修改图片信息
     */
    @PostMapping("/edit/batch")
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }
    /**
     * 创建图片外绘任务
     */
    @PostMapping("/out_painting/creat_task")
    public BaseResponse<CreateOutPaintingTaskResponse> creatPictureOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null ||createPictureOutPaintingTaskRequest.getPictureId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse createOutPaintingTaskResponse = pictureService.creatPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(createOutPaintingTaskResponse);
    }

    /**
     * 查询图片外绘任务
     * @param taskId
     * @param request
     * @return
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId, HttpServletRequest request){
        ThrowUtils.throwIf(taskId == null, ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }
    /**
     * 获取原图接口
     */
    @GetMapping("/get_original_picture")
    public BaseResponse<String> getOriginalPicture(@RequestParam("pictureId") Long pictureId, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        String originalPicture = pictureService.getOriginalPicture(pictureId, loginUser);
        return ResultUtils.success(originalPicture);
    }
}
