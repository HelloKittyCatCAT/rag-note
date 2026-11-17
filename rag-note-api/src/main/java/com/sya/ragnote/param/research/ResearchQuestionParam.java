package com.sya.ragnote.param.research;

import com.sya.ragnote.param.chat.AskQuestionParam;
import java.util.UUID;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResearchQuestionParam extends AskQuestionParam {
    private boolean allowWeb;

    /** 本轮明确授权读取的公开链接，最多三个。 */
    @Size(max = 3)
    private List<@NotBlank @Size(max = 2048) String> webUrls;
    private UUID conversationId;
}
