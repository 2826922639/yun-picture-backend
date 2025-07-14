package com.hhh.yunpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;
@Data
public class UserAvatarRequest implements Serializable {
    private static final long serialVersionUID = -2396790991899900465L;
    /**
     * 用户id
     */
    private Long id;
    /**
     * 用户头像url
     */
    private String userAvatarUrl;
}
