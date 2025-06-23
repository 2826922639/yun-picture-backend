package com.hhh.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hhh.yunpicturebackend.model.dto.user.UserLoginRequest;
import com.hhh.yunpicturebackend.model.dto.user.UserQueryRequest;
import com.hhh.yunpicturebackend.model.dto.user.UserRegisterRequest;
import com.hhh.yunpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhh.yunpicturebackend.model.vo.LoginUserVO;
import com.hhh.yunpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author shoto
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-06-18 12:57:36
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userRegisterRequest 用户注册请求
     * @return 用户id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);
    /**
     * 获取加密密码
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);
    /**
     * 用户登录
     * @param userLoginRequest 用户登录请求
     * @return 脱敏后的登录用户信息
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);
    /**
     * 获取当前登录用户
     * @param request 请求
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest request);
    /**
     * 退出登录
     * @param request 请求
     * @return 退出登录结果
     */
    boolean logoutUser(HttpServletRequest request);
    /**
     * 获取脱敏后的登录用户信息
     * @param user 用户
     * @return 脱敏后的登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);
    /**
     * 获取脱敏后的用户信息
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    UserVO getUserVO(User user);
    /**
     * 获取脱敏后的用户信息列表
     * @param userList 用户列表
     * @return 脱敏后的用户列表信息
     */
    List<UserVO> getUserVOList(List< User> userList);
    /**
     * 获取查询条件
     * @param userQueryRequest 用户查询请求
     * @return 查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
    /**
     * 是否为管理员
     * @return 用户
     */
    boolean isAdmin(User user);
}
