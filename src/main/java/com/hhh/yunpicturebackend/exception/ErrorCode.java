package com.hhh.yunpicturebackend.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0,"ok"),
    PARAMS_ERROR(40000,"请求参数错误！"),
    NULL_ERROR(40001,"请求数据为空！"),
    NOT_LOGIN(40100,"未登录！"),
    NO_AUTH(40101,"无权限"),
    NOT_FOUND_ERROR(40400,"请求数据不存在"),
    FORBIDDEN(40300,"禁止访问"),
    SYSTEM_ERROR(50000,"系统内部异常"),
    OPERATION_ERROR(50001,"操作失败"),
    TOO_MANY_REQUEST(42900, "请求过于频繁");
    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
