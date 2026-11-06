package com.sya.ragnote.client.embedding;

import java.util.List;

public interface EmbeddingClient {
    String modelId();

    float[] embed(String text);

    List<float[]> embedAll(List<String> texts);
}
