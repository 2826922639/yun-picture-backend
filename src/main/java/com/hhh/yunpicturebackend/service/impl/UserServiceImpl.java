package com.hhh.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hhh.yunpicturebackend.constant.UserConstant;
import com.hhh.yunpicturebackend.exception.BusinessException;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.exception.ThrowUtils;
import com.hhh.yunpicturebackend.manager.auth.StpKit;
import com.hhh.yunpicturebackend.model.dto.user.UserLoginRequest;
import com.hhh.yunpicturebackend.model.dto.user.UserQueryRequest;
import com.hhh.yunpicturebackend.model.dto.user.UserRegisterRequest;
import com.hhh.yunpicturebackend.model.entity.User;
import com.hhh.yunpicturebackend.model.enums.UserRoleEnum;
import com.hhh.yunpicturebackend.model.vo.LoginUserVO;
import com.hhh.yunpicturebackend.model.vo.UserVO;
import com.hhh.yunpicturebackend.service.UserService;
import com.hhh.yunpicturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.hhh.yunpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author shoto
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-06-18 12:57:36
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{
    /**
     * 用户注册
     * @param userRegisterRequest 用户注册请求
     * @return 用户id
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空！");
        }
        ThrowUtils.throwIf(userAccount.length() < 4,ErrorCode.PARAMS_ERROR,"用户账号过短！");
        ThrowUtils.throwIf(userAccount.length() > 16,ErrorCode.PARAMS_ERROR,"用户账号过长！");
        ThrowUtils.throwIf(userPassword.length() < 8 || checkPassword.length() < 8,ErrorCode.PARAMS_ERROR,"用户密码过短！");
        ThrowUtils.throwIf(userPassword.equals(userAccount),ErrorCode.PARAMS_ERROR,"用户密码不能和账号相同！");
        ThrowUtils.throwIf(userPassword.length() > 16 || checkPassword.length() > 16,ErrorCode.PARAMS_ERROR,"用户密码过长！");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword),ErrorCode.PARAMS_ERROR,"两次输入的密码不一致！");
        //2.检查用户账号是否和数据库中已有的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        ThrowUtils.throwIf(count > 0,ErrorCode.PARAMS_ERROR,"用户账号已存在！");
        //3.密码需要加密
        String encryptPassword = getEncryptPassword(userPassword);
        //4.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("嘿嘿嘿");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = this.save(user);
        ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR,"注册失败！");
        return user.getId();
    }
    /**
     * 获取加密密码
     * @param userPassword 用户输入的密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword){
        //混淆密码
        final String salt = "yunpicturebackendhhhzgwyu";
        return DigestUtils.md5DigestAsHex((salt+userPassword).getBytes());
    }
    /**
     * 用户登录
     * @param userLoginRequest 用户登录请求
     * @return 脱敏后的登录用户信息
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        //1.校验参数
        ThrowUtils.throwIf(userAccount.length() < 4,ErrorCode.PARAMS_ERROR,"用户账号过短！");
        ThrowUtils.throwIf(userAccount.length() > 16,ErrorCode.PARAMS_ERROR,"用户账号过长！");
        ThrowUtils.throwIf(userPassword.length() < 8 ,ErrorCode.PARAMS_ERROR,"用户密码过短！");
        ThrowUtils.throwIf(userPassword.length() > 16,ErrorCode.PARAMS_ERROR,"用户密码过长！");
        //2.对用户输入密码进行加密
        String encryptPassword = getEncryptPassword(userPassword);
        //3.查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        User user = this.baseMapper.selectOne(queryWrapper);
        //4.用户不存在则返回错误
        ThrowUtils.throwIf(user == null,ErrorCode.NULL_ERROR,"用户不存在！");
        //5.如果存在，则判断密码是否正确
        queryWrapper.eq("userPassword",encryptPassword);
        user = this.baseMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(user == null,ErrorCode.PARAMS_ERROR,"用户密码错误！");
        //6.保存用户登录状态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE,user);
        //记录用户登录态到Sa-token，便于空间鉴权时使用，注意保证用户信息与SpringSession中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE,user);
        return this.getLoginUserVO(user);
    }
    /**
     * 获取脱敏后的登录用户信息
     * @param user 登录用户
     * @return 脱敏后的登录用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null){
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user,loginUserVO);
        return loginUserVO;
    }
    /**
     * 获取当前登录用户
     * @param request 请求
     * @return 当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        ThrowUtils.throwIf(currentUser == null||currentUser.getId()==null,ErrorCode.NOT_LOGIN);
        //追求性能可以直接返回
        Long id = currentUser.getId();
        currentUser=this.getById(id);
        ThrowUtils.throwIf(currentUser == null,ErrorCode.NOT_LOGIN);
        return currentUser;
    }
    /**
     * 退出登录
     * @param request 请求
     * @return 退出登录结果
     */
    @Override
    public boolean logoutUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        ThrowUtils.throwIf(userObj == null,ErrorCode.OPERATION_ERROR,"未登录！");
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }
    /**
     * 获取脱敏后的用户信息
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null){
            return null;
        }
        UserVO UserVO = new UserVO();
        BeanUtil.copyProperties(user,UserVO);
        return UserVO;
    }
    /**
     * 获取脱敏后的用户信息列表
     * @param userList 用户列表
     * @return 脱敏后的用户列表信息
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }
    /**
     * 获取查询条件
     * @param userQueryRequest 用户查询请求
     * @return 查询条件
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null,ErrorCode.PARAMS_ERROR,"请求参数为空！");
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull( id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank( userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank( userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank( userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank( userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),sortOrder.equals("ascend"),sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}




