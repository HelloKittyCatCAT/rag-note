package com.sya.ragnote.config;

import com.sya.ragnote.service.system.DatabaseSchemaService;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(-100)
public class DatabaseSchemaInitializer implements ApplicationRunner {
    private final DatabaseSchemaService schema;

    public DatabaseSchemaInitializer(DatabaseSchemaService schema) {
        this.schema = schema;
    }

    public void run(ApplicationArguments arguments) {
        schema.verifyEmbeddingDimensions();
    }
}
