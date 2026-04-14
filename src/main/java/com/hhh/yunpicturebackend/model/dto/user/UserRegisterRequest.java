package com.hhh.yunpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 * @author HHH
 * @date 2023/10/15
 */
@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = -6432198567435107160L;
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 密码
     */
    private String userPassword;
    /**
     * 校验密码
     */
    private String checkPassword;
}
