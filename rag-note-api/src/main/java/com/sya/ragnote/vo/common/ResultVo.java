package com.sya.ragnote.vo.common;


/** 统一响应；requestId由Web入口生成。 */
public record ResultVo<T>(int code, String msg, T data, String requestId) {
}
