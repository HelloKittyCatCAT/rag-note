package com.sya.ragnote.tokenizer;

import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

import org.springframework.stereotype.Component;

/** 固定 cl100k_base tokenizer；与供应商实际计费 Token 数可能不同。 */
@Component
public class TokenCounter {
    private final OpenAiTokenCountEstimator tokenizer = new OpenAiTokenCountEstimator("gpt-4");

    public int count(String text) {
        return tokenizer.estimateTokenCountInText(text);
    }
}
