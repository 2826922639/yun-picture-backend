package com.hhh.yunpicturebackend.api.imagesearch.model;

import lombok.Data;

/**
 * 图片搜索结果
 */
@Data
public class ImageSearchResult {
    /**
     * 图片缩略图地址
     */
    private String thumbUrl;
    /**
     * 图片来源地址
     */
    private String fromUrl;
}
