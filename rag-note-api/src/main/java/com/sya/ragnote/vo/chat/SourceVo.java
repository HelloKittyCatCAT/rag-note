package com.sya.ragnote.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.UUID;

/** 笔记回答来源 */
@Data
@Schema(description = "笔记回答来源")
public class SourceVo {
    @Schema(description = "本次证据编号S1等")
    private String sourceId;

    @Schema(description = "笔记编号")
    private UUID noteId;

    @Schema(description = "分块编号")
    private UUID chunkId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "章节路径")
    private String headingPath;

    @Schema(description = "原文起始行，从1开始")
    private int startLine;

    @Schema(description = "原文结束行")
    private int endLine;

    @Schema(description = "最多500字符的摘要片段")
    @lombok.ToString.Exclude
    private String snippet;

    @Schema(description = "余弦相似度，不代表正确概率")
    private double score;

}
