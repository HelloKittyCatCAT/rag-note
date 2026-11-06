package com.sya.ragnote.controller.note;

import com.sya.ragnote.enums.result.CommonResultEnum;
import com.sya.ragnote.enums.result.NoteResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.param.note.*;
import com.sya.ragnote.security.LoginContext;
import com.sya.ragnote.service.note.NoteService;
import com.sya.ragnote.vo.common.*;
import com.sya.ragnote.vo.note.*;
import com.sya.ragnote.web.Responses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.*;
import java.util.UUID;

@Tag(name = "笔记", description = "本人Markdown笔记上传与管理")
@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService service;
    private final Responses responses;

    public NoteController(NoteService service, Responses responses) {
        this.service = service;
        this.responses = responses;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传单个Markdown笔记")
    public ResultVo<NoteVo> upload(
            @RequestPart("file") MultipartFile file,
            @Valid @ModelAttribute UploadNoteParam param,
            MultipartHttpServletRequest request) {
        if (request.getMultiFileMap().size() != 1
                || !request.getMultiFileMap().containsKey("file")
                || request.getFiles("file").size() != 1)
            throw new ServiceSystemException(CommonResultEnum.INVALID_PARAMETER);
        if (request.getParameterMap().keySet().stream().anyMatch(key -> !key.equals("title"))
                || request.getParameterValues("title") != null
                        && request.getParameterValues("title").length != 1)
            throw new ServiceSystemException(CommonResultEnum.INVALID_PARAMETER);
        try {
            for (var part : request.getParts()) {
                if (!part.getName().equals("file") && !part.getName().equals("title"))
                    throw new ServiceSystemException(CommonResultEnum.INVALID_PARAMETER);
            }
        } catch (IOException | jakarta.servlet.ServletException e) {
            throw new ServiceSystemException(CommonResultEnum.INVALID_PARAMETER, e);
        }
        try (InputStream in = file.getInputStream()) {
            return responses.ok(
                    service.upload(LoginContext.userId(), param, in, file.getOriginalFilename()));
        } catch (IOException e) {
            throw new ServiceSystemException(NoteResultEnum.INVALID_FILE_ENCODING, e);
        }
    }

    @Operation(summary = "分页查询本人笔记")
    @GetMapping
    public ResultVo<PageVo<NoteVo>> list(@Valid @ModelAttribute QueryNoteParam param) {
        return responses.ok(service.list(LoginContext.userId(), param));
    }

    @Operation(summary = "查询本人笔记详情")
    @GetMapping("/detail")
    public ResultVo<NoteDetailVo> detail(@RequestParam("noteId") UUID id) {
        return responses.ok(service.detail(LoginContext.userId(), id));
    }

    @Operation(summary = "删除本人笔记及分块")
    @DeleteMapping
    public ResultVo<Void> delete(@RequestParam("noteId") UUID id) {
        service.delete(LoginContext.userId(), id);
        return responses.ok(null);
    }
}
