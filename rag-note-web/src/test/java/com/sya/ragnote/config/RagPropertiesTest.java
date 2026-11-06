package com.sya.ragnote.config;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

class RagPropertiesTest {
    @Test
    void endpointsAllowNormalHostnamesButRejectWhitespace() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            RagProperties.Model model = new RagProperties.Model();
            model.setApiKey("test-only-key");
            model.setModel("configured-model");
            model.setBaseUrl("https://dashscope.example.com/compatible-mode/v1");
            assertTrue(validator.validate(model).isEmpty());
            model.setBaseUrl("http://localhost:8081/v1");
            assertTrue(validator.validate(model).isEmpty());
            model.setBaseUrl("https://bad host.example/v1");
            assertFalse(validator.validate(model).isEmpty());
        }
    }
}
