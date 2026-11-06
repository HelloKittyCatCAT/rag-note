package com.sya.ragnote.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.Instant;

/** 邀请码，仅创建时返回一次 */
@Data
@Schema(description = "邀请码，仅创建时返回一次")
public class InvitationVo {
    @Schema(description = "邀请码明文")
    @lombok.ToString.Exclude
    private String token;

    @Schema(description = "过期时间，UTC")
    private Instant expiresAt;

}
