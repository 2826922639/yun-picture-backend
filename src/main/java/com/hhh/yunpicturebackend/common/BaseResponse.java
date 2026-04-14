package com.hhh.yunpicturebackend.common;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局响应封装类
 * @param <T>
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class BaseResponse<T> implements Serializable {
    private  int code;
    private T data;
    private String message;

    public BaseResponse(int code, T data,String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }
    public BaseResponse(int code, T data) {
        this(code,data,"");
    }
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(),null,errorCode.getMessage());
    }
}
