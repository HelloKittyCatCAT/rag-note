package com.sya.ragnote.param.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;

/** 管理员修改用户状态 */
@Data
@Schema(description = "管理员修改用户状态")
public class UpdateUserStatusParam {
    @Schema(description = "用户编号，使用字符串避免前端精度丢失")
    @NotBlank
    @Pattern(regexp = "[1-9][0-9]{0,18}")
    @DecimalMax("9223372036854775807")
    private String userId;

    @Schema(description = "是否启用")
    @NotNull
    private Boolean enabled;

}
