package com.sya.ragnote.param.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户分页条件 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户分页条件")
public class QueryUserParam extends com.sya.ragnote.param.common.PageParam {
}
