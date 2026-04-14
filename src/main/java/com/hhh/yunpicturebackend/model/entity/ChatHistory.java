package com.hhh.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *  实体类。
 *
 * @author <a href="https://www.zgwyu.online/">嘿嘿嘿</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_history")
public class ChatHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 消息
     */
    private String message;

    /**
     * user/ai
     */
    //@TableField("messageType")
    private String messageType;

    /**
     * 应用id
     */
    //@TableField("appId")
    private Long appId;

    /**
     * 创建用户id
     */
    //@TableField("userId")
    private Long userId;

    /**
     * 创建时间
     */
    //@TableField("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    //@TableField("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

}
