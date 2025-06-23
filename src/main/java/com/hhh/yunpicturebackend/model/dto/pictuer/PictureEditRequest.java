package com.hhh.yunpicturebackend.model.dto.pictuer;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 修改图片信息请求（给用户使用）
 */
@Data
public class PictureEditRequest implements Serializable {
  
    /**  
     * id  
     */  
    private Long id;  
  
    /**  
     * 图片名称  
     */  
    private String name;  
  
    /**  
     * 简介  
     */  
    private String introduction;  
  
    /**  
     * 分类  
     */  
    private String category;  
  
    /**  
     * 标签  
     */  
    private List<String> tags;
  
    private static final long serialVersionUID = 1L;  
}
