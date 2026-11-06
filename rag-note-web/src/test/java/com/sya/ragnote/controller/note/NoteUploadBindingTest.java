package com.sya.ragnote.controller.note;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.enums.result.CommonResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.param.note.UploadNoteParam;
import com.sya.ragnote.service.note.NoteService;
import com.sya.ragnote.web.Responses;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.*;

import java.util.*;

class NoteUploadBindingTest {
    @Test
    void duplicateFilePartIsRejectedBeforeService() {
        var service = mock(NoteService.class);
        var request = mock(MultipartHttpServletRequest.class);
        var file = mock(MultipartFile.class);
        var files = new LinkedMultiValueMap<String, MultipartFile>();
        files.put("file", List.of(file, file));
        when(request.getMultiFileMap()).thenReturn(files);
        when(request.getFiles("file")).thenReturn(files.get("file"));
        var error =
                assertThrows(
                        ServiceSystemException.class,
                        () ->
                                new NoteController(service, mock(Responses.class))
                                        .upload(file, new UploadNoteParam(), request));
        assertSame(CommonResultEnum.INVALID_PARAMETER, error.getResultCode());
        verifyNoInteractions(service);
    }

    @Test
    void unknownFormFieldIsRejectedBeforeService() {
        var service = mock(NoteService.class);
        var request = mock(MultipartHttpServletRequest.class);
        var file = mock(MultipartFile.class);
        var files = new LinkedMultiValueMap<String, MultipartFile>();
        files.put("file", List.of(file));
        when(request.getMultiFileMap()).thenReturn(files);
        when(request.getFiles("file")).thenReturn(files.get("file"));
        when(request.getParameterMap()).thenReturn(Map.of("ownerId", new String[] {"2"}));
        var error =
                assertThrows(
                        ServiceSystemException.class,
                        () ->
                                new NoteController(service, mock(Responses.class))
                                        .upload(file, new UploadNoteParam(), request));
        assertSame(CommonResultEnum.INVALID_PARAMETER, error.getResultCode());
        verifyNoInteractions(service);
    }
}
