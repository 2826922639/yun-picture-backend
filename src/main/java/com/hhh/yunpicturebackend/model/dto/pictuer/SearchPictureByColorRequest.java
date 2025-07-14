package com.hhh.yunpicturebackend.model.dto.pictuer;

import lombok.Data;

import java.io.Serializable;

/**
 * 按照颜色搜索请求
 */
@Data
public class SearchPictureByColorRequest implements Serializable {
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 图片主色调
     */
    private String picColor;

    private static final long serialVersionUID = 1L;
}
