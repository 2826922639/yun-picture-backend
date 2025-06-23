package com.hhh.yunpicturebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用删除请求类
 */
@Data
public class DeleteRequeat implements Serializable {
    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID=1L;
}
