package com.sya.ragnote.dao.impl.system;

import com.sya.ragnote.dao.system.DatabaseSchemaDao;
import com.sya.ragnote.mapper.system.DatabaseSchemaMapper;

import org.springframework.stereotype.Repository;

@Repository
public class DatabaseSchemaDaoImpl implements DatabaseSchemaDao {
    private final DatabaseSchemaMapper mapper;

    public DatabaseSchemaDaoImpl(DatabaseSchemaMapper mapper) {
        this.mapper = mapper;
    }

    public Integer embeddingDimensions() {
        return mapper.embeddingDimensions();
    }
}
