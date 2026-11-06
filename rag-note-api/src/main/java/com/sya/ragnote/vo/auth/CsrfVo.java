package com.sya.ragnote.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 写请求使用的CSRF令牌 */
@Data
@Schema(description = "写请求使用的CSRF令牌")
public class CsrfVo {
    @Schema(description = "令牌值")
    @lombok.ToString.Exclude
    private String token;

    @Schema(description = "请求头名称")
    private String headerName;

}
