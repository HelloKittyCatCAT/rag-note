package com.sya.ragnote.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/** 笔记问答结果 */
@Data
@Schema(description = "笔记问答结果")
public class AskQuestionVo {
    @Schema(description = "回答正文")
    private String answer;

    @Schema(description = "结果状态")
    private com.sya.ragnote.enums.chat.AnswerStatus status;

    @Schema(description = "经校验的本人笔记来源")
    private List<SourceVo> sources = List.of();

}
