package com.sya.ragnote.service.research;

import com.sya.ragnote.param.research.ResearchQuestionParam;
import com.sya.ragnote.vo.research.ResearchAnswerVo;

public interface ResearchService {
    ResearchAnswerVo ask(long ownerId, ResearchQuestionParam param);
}
