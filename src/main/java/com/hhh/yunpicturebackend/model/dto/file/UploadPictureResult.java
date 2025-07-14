package com.hhh.yunpicturebackend.model.dto.file;

import lombok.Data;

@Data
public class UploadPictureResult {  
  
    /**  
     * 图片地址  
     */  
    private String url;
    /**
     * 图片首次缩略地址
     */
    private String sUrl;
    /**
     * 图片首次缩略key
     */
    private String sKey;
    /**  
     * 图片名称  
     */  
    private String picName;  
  
    /**  
     * 文件体积  
     */  
    private Long picSize;  
  
    /**  
     * 图片宽度  
     */  
    private int picWidth;  
  
    /**  
     * 图片高度  
     */  
    private int picHeight;  
  
    /**  
     * 图片宽高比  
     */  
    private Double picScale;  
  
    /**  
     * 图片格式  
     */  
    private String picFormat;
    /**
     * 图片key
     */
    private String cosKey;
    /**
     * 缩略图key
     */
    private String thumbnailKey;
    /**
     * 缩略图 url
     */
    private String thumbnailUrl;
    /**
     * 图片颜色
     */
    private String picColor;
}
