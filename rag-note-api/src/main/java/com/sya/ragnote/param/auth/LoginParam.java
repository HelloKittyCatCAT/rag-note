package com.sya.ragnote.param.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 登录参数 */
@Data
@Schema(description = "登录参数")
public class LoginParam {
    @Schema(description = "用户名：3—32位字母、数字或下划线")
    @NotBlank
    @Pattern(regexp = "[a-zA-Z0-9_]{3,32}")
    private String username;

    @Schema(description = "密码：12—64字符；服务端另检查UTF-8不超过72字节")
    @NotBlank
    @Size(min = 12, max = 64)
    @lombok.ToString.Exclude
    private String password;

}
