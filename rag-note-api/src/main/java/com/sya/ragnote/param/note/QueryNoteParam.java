package com.sya.ragnote.param.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 本人笔记分页条件 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "本人笔记分页条件")
public class QueryNoteParam extends com.sya.ragnote.param.common.PageParam {
}
