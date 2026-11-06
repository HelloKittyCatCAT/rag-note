package com.sya.ragnote.vo.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 本人笔记详情 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "本人笔记详情")
public class NoteDetailVo extends NoteVo {
    @Schema(description = "Markdown原文，展示时必须清洗HTML")
    @lombok.ToString.Exclude
    private String rawMarkdown;

    @Schema(description = "解析正文")
    @lombok.ToString.Exclude
    private String plainText;

}
