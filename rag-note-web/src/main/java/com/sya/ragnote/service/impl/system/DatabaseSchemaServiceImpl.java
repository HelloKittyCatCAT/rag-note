package com.sya.ragnote.service.impl.system;

import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.dao.system.DatabaseSchemaDao;
import com.sya.ragnote.enums.result.NoteResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.service.system.DatabaseSchemaService;

import org.springframework.stereotype.Service;

@Service
public class DatabaseSchemaServiceImpl implements DatabaseSchemaService {
    private final DatabaseSchemaDao schema;
    private final RagProperties properties;

    public DatabaseSchemaServiceImpl(DatabaseSchemaDao schema, RagProperties properties) {
        this.schema = schema;
        this.properties = properties;
    }

    public void verifyEmbeddingDimensions() {
        Integer dimensions = schema.embeddingDimensions();
        if (dimensions == null
                || dimensions != properties.getModels().getEmbedding().getDimensions()) {
            throw new ServiceSystemException(NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
        }
    }
}
