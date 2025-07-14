package com.hhh.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hhh.yunpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.hhh.yunpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.hhh.yunpicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.model.vo.SpaceUserVO;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author shoto
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-07-14 15:42:45
*/
public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 添加空间成员
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);
    /**
     * 校验空间成员
     *
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间成员包装类（脱敏，单条）
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);
    /**
     * 获取空间成员包装类（脱敏，多条）
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
    /**
     * 获取空间成员查询条件
     * @param spaceUserQueryRequest 空间查询请求
     * @return 查询条件
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

}
