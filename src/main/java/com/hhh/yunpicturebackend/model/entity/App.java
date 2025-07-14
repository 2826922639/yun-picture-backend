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
@TableName("app")
public class App implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 应用名称
     */
    @TableField("appName")
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 应用初始化的 prompt
     */
    @TableField("initPrompt")
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     */
    @TableField("codeGenType")
    private String codeGenType;

    /**
     * 部署标识
     */
    @TableField("deployKey")
    private String deployKey;

    /**
     * 部署时间
     */
    @TableField("deployedTime")
    private LocalDateTime deployedTime;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建用户id
     */
    @TableField("userId")
    private Long userId;

    /**
     * 编辑时间
     */
    @TableField("editTime")
    private LocalDateTime editTime;

    /**
     * 创建时间
     */
    @TableField("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

}
