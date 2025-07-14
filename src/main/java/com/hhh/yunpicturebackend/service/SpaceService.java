package com.hhh.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhh.yunpicturebackend.model.dto.pictuer.PictureQueryRequest;
import com.hhh.yunpicturebackend.model.dto.space.SpaceAddRequest;
import com.hhh.yunpicturebackend.model.dto.space.SpaceQueryRequest;
import com.hhh.yunpicturebackend.model.entity.Picture;
import com.hhh.yunpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.model.vo.PictureVO;
import com.hhh.yunpicturebackend.model.vo.SpaceVO;

import jakarta.servlet.http.HttpServletRequest;

/**
* @author shoto
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-06-30 15:39:53
*/
public interface SpaceService extends IService<Space> {
    /**
     * 添加空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);
    /**
     * 校验
     *
     * @param space
     * @param add
     */
    void validSpace(Space space, boolean add);

    /**
     * 填充空间信息
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);
    /**
     * 获取空间包装类（脱敏，单条）
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space  space, HttpServletRequest request);
    /**
     * 获取空间包装类（脱敏，多条）
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);
    /**
     * 获取查询条件
     * @param spaceQueryRequest 空间查询请求
     * @return 查询条件
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);
    /**
     * 校验空间权限
     *
     * @param space
     * @param loginUser
     */
    void checkSpaceAuth(User loginUser,Space  space);
}
