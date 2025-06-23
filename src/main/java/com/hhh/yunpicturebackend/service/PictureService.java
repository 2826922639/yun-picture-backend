package com.hhh.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhh.yunpicturebackend.model.dto.pictuer.PictureQueryRequest;
import com.hhh.yunpicturebackend.model.dto.pictuer.PictureUploadRequest;
import com.hhh.yunpicturebackend.model.dto.user.UserQueryRequest;
import com.hhh.yunpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author shoto
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-06-21 23:44:45
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     * @param multipartFile  文件
     * @param pictureUploadRequest 上传图片信息
     * @return 上传图片结果
     */
    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

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
     * 删除图片
     * @param id
     * @param loginUser
     * @return
     */
    Boolean deletePicture(Long id, User loginUser);
}
