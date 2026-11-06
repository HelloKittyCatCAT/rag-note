package com.sya.ragnote.param.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 上传笔记的附加字段；文件由multipart附件传入 */
@Data
@Schema(description = "上传笔记的附加字段；文件由multipart附件传入")
public class UploadNoteParam {
    @Schema(description = "可选标题；不传时从文档或文件名提取")
    @Size(min = 1, max = 200)
    @Pattern(regexp = "(?s).*\\S.*", message = "标题不能全为空白")
    private String title;

}
