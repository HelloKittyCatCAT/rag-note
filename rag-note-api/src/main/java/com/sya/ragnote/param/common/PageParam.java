package com.sya.ragnote.param.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/** 分页条件 */
@Data
@Schema(description = "分页条件")
public class PageParam {
    @Schema(description = "页码，从1开始")
    @Min(1)
    private int page = 1;

    @Schema(description = "每页条数，最大100")
    @Min(1)
    @Max(100)
    private int size = 20;

}
