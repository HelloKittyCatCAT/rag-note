package com.sya.ragnote.service.system;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.dao.system.DatabaseSchemaDao;
import com.sya.ragnote.enums.result.NoteResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.service.impl.system.DatabaseSchemaServiceImpl;

import org.junit.jupiter.api.Test;

class DatabaseSchemaServiceTest {
    @Test
    void startupRefusesMissingOrDriftedVectorColumn() {
        var dao = mock(DatabaseSchemaDao.class);
        var service = new DatabaseSchemaServiceImpl(dao, new RagProperties());
        for (Integer actual : new Integer[] {null, 1536, -1}) {
            when(dao.embeddingDimensions()).thenReturn(actual);
            assertEquals(
                    NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH,
                    assertThrows(ServiceSystemException.class, service::verifyEmbeddingDimensions)
                            .getResultCode());
        }
    }

    @Test
    void startupAcceptsMatchingDatabaseDimensions() {
        var dao = mock(DatabaseSchemaDao.class);
        when(dao.embeddingDimensions()).thenReturn(1024);
        assertDoesNotThrow(
                () ->
                        new DatabaseSchemaServiceImpl(dao, new RagProperties())
                                .verifyEmbeddingDimensions());
    }
}
