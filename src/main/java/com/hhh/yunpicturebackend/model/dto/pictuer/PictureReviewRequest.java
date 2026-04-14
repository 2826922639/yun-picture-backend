package com.hhh.yunpicturebackend.model.dto.pictuer;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @Description: 图片审核请求（给管理员使用）
 */
@Data
public class PictureReviewRequest implements Serializable {

    /**
     * id
     */
    private Long id;
    /**
     * 审核状态;0:待审核;1:审核通过;2:审核未通过
     */
    private Integer reviewStatus;
    /**
     * 审核信息
     */
    private String reviewMessage;
  
    private static final long serialVersionUID = 1L;  
}
