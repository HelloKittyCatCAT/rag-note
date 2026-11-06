package com.sya.ragnote.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 用户公开信息 */
@Data
@Schema(description = "用户公开信息")
public class UserVo {
    @Schema(description = "用户编号字符串")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "角色")
    private com.sya.ragnote.enums.user.UserRole role;

    @Schema(description = "是否启用")
    private boolean enabled;

}
