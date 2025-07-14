package com.hhh.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhh.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.hhh.yunpicturebackend.common.DeleteRequest;
import com.hhh.yunpicturebackend.model.dto.pictuer.*;
import com.hhh.yunpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.model.vo.PictureVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author shoto
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-06-21 23:44:45
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     * @param inputSource  文件输入源
     * @param pictureUploadRequest 上传图片信息
     * @return 上传图片结果
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 获取图片包装类（脱敏，单条）
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);
    /**
     * 获取图片包装类（脱敏，多条）
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 获取查询条件
     * @param pictureQueryRequest 图片查询请求
     * @return 查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);
    /**
     * 图片校验
     * @param picture
     */
    void validPicture(Picture picture);
    /**
     * 删除对象存储的图片
     * @param id
     * @param loginUser
     * @return
     */
    Boolean deletePicture(Long id, User loginUser);
    /**
     * 图片审核
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser 登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);
    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);
    /**
     * 批量上传图片
     * @param pictureUploadByBatchRequest 图片批量上传请求
     * @param loginUser 登录用户
     * @return
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);
    /**
     * 检查图片权限
     * @param loginUser 登录用户
     * @param picture 图片
     */
    void checkPictureAuth(User loginUser,Picture  picture);
    /**
     * 删除图片
     * @param deleteRequest 删除请求
     * @param loginUser 登录用户
     * @return
     */
    Boolean delete(DeleteRequest deleteRequest, User loginUser);
    /**
     * 更新图片信息
     * @param pictureUpdateRequest 图片更新请求
     * @param loginUser 登录用户
     * @return
     */
    Boolean updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser);
    /**
     * 编辑图片信息
     * @param pictureEditRequest 图片编辑请求
     * @param loginUser 登录用户
     * @return
     */
    Boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser);
    /**
     * 根据颜色搜索图片
     */
    List<PictureVO>searchPictureByColor(Long soaceId, String picColor, User loginUser);
    /**
     * 批量编辑图片信息
     * @param pictureEditByBatchRequest 图片批量编辑请求
     * @param loginUser 登录用户
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);
    /**
     * 创建图片扩图任务
     */
    CreateOutPaintingTaskResponse creatPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest , User loginUser);
    /**
     * 获取图片原始图片(下载，裁剪，ai扩图)
     */
    String getOriginalPicture(Long pictureId, User loginUser);
}
