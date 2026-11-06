package com.sya.ragnote.param.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** 仅依据本人笔记的单轮问答 */
@Data
@Schema(description = "仅依据本人笔记的单轮问答")
public class AskQuestionParam {
    @Schema(description = "用户问题")
    @NotBlank
    @Size(max = 2000)
    @lombok.ToString.Exclude
    private String question;

    @Schema(description = "限定笔记范围；null为本人全库，显式空集合非法")
    @Size(min = 1, max = 20)
    private List<@NotNull UUID> noteIds;

}
