package com.hhh.yunpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhh.yunpicturebackend.annotation.AuthCheck;
import com.hhh.yunpicturebackend.common.BaseResponse;
import com.hhh.yunpicturebackend.common.DeleteRequest;
import com.hhh.yunpicturebackend.common.ResultUtils;
import com.hhh.yunpicturebackend.constant.UserConstant;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.exception.ThrowUtils;
import com.hhh.yunpicturebackend.model.dto.user.*;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.model.enums.UserRoleEnum;
import com.hhh.yunpicturebackend.model.vo.LoginUserVO;
import com.hhh.yunpicturebackend.model.vo.UserVO;
import com.hhh.yunpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        ThrowUtils.throwIf(userRegisterRequest==null, ErrorCode.PARAMS_ERROR);
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }
    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        ThrowUtils.throwIf(userLoginRequest==null, ErrorCode.PARAMS_ERROR);
        LoginUserVO userVO = userService.userLogin(userLoginRequest, request);
        return ResultUtils.success(userVO);
    }
    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest  request){
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }
    /**
     * 用户注销
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logoutUser(HttpServletRequest request){
        return ResultUtils.success(userService.logoutUser(request));
    }
    /**
     * 添加用户
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest){
        ThrowUtils.throwIf(userAddRequest==null, ErrorCode.PARAMS_ERROR);
        User user=new User();
        BeanUtil.copyProperties(userAddRequest,user);
        final String DEFAULT_PASSWORD="12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save( user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }
    /**
     * 根据id获取用户
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id){
        ThrowUtils.throwIf(id<=0,ErrorCode.PARAMS_ERROR);
        User User = userService.getById(id);
        return ResultUtils.success(User);
    }
    /**
     * 根据id获取包装类
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id){
        BaseResponse<User> result = getUserById(id);
        User user = result.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }
    /**
     * 删除用户
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        ThrowUtils.throwIf(deleteRequest ==null|| deleteRequest.getId()<=0,ErrorCode.PARAMS_ERROR);
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }
    /**
     * 更新用户
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,HttpServletRequest request){
        ThrowUtils.throwIf(userUpdateRequest==null, ErrorCode.PARAMS_ERROR);
        User user=new User();
        User loginUser = userService.getLoginUser(request);
        if(userService.isAdmin(loginUser)){
            ThrowUtils.throwIf(userUpdateRequest.getId().equals(loginUser.getId())&&userUpdateRequest.getUserRole().equals(UserConstant.DEFAULT_ROLE),ErrorCode.NO_AUTH,"不能修改自己的角色！");
            BeanUtil.copyProperties(userUpdateRequest,user);
        } else {
            user.setId(loginUser.getId());
            user.setUserAccount(userUpdateRequest.getUserAccount());
            user.setUserName(userUpdateRequest.getUserName());
            user.setUserAvatar(userUpdateRequest.getUserAvatar());
            user.setUserProfile(userUpdateRequest.getUserProfile());
            ThrowUtils.throwIf(userUpdateRequest.getUserRole().equals(UserConstant.ADMIN_ROLE),ErrorCode.NO_AUTH,"无权限操作用户角色！");

        }
        boolean result = userService.updateById( user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }
    /**
     * 获取用户列表
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest){
        ThrowUtils.throwIf(userQueryRequest==null,ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pagesize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pagesize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current,pagesize,userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
    /**
     * 获取当前用户
     * @param request
     * @return
     */
    @GetMapping("/current")
    public BaseResponse<UserVO> getCurrentUser(HttpServletRequest request){
        User currentUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getUserVO(currentUser));
    }
    /**
     * 头像
     */
    @PostMapping("/set/avatar")
    public BaseResponse<Boolean> setAvatar(@RequestBody UserAvatarRequest avatarUrl, HttpServletRequest request){
        ThrowUtils.throwIf(avatarUrl == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!loginUser.getId().equals(avatarUrl.getId()), ErrorCode.NO_AUTH);
        boolean result = userService.update(new UpdateWrapper<User>().set("userAvatar",avatarUrl.getUserAvatarUrl()).eq("id",avatarUrl.getId()));
        return ResultUtils.success(result);
    }
}
