package com.hhh.yunpicturebackend.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;
/**
 * @description：空间等级
 */
@Data
@AllArgsConstructor
public class SpaceLevel {
    /**
     * 等级值
     */
    private int value;
    /**
     * 等级名称
     */
    private String text;
    /**
     * 最大数量
     */
    private long maxCount;
    /**
     * 最大容量
     */
    private long maxSize;
}
