package com.sya.ragnote.contract;

import static org.junit.jupiter.api.Assertions.*;

import com.sya.ragnote.param.chat.AskQuestionParam;
import com.sya.ragnote.param.note.QueryNoteParam;
import com.sya.ragnote.param.note.UploadNoteParam;
import com.sya.ragnote.param.user.UpdateUserStatusParam;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

class RequestValidationTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final Validator validator = FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        FACTORY.close();
    }

    @Test
    void questionMustHaveText() {
        AskQuestionParam param = new AskQuestionParam();
        param.setQuestion("   ");
        assertTrue(
                validator.validate(param).stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("question")));
    }

    @Test
    void explicitEmptyNoteScopeMustNotBecomeAllNotes() {
        AskQuestionParam param = question();
        param.setNoteIds(List.of());
        assertTrue(
                validator.validate(param).stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("noteIds")));
    }

    @Test
    void missingNoteScopeMeansAllOwnedNotes() {
        assertTrue(validator.validate(question()).isEmpty());
    }

    @Test
    void nullNoteIdIsRejected() {
        AskQuestionParam param = question();
        param.setNoteIds(Arrays.asList(UUID.randomUUID(), null));
        assertFalse(validator.validate(param).isEmpty());
    }

    @Test
    void oversizedPageIsRejected() {
        QueryNoteParam param = new QueryNoteParam();
        param.setSize(101);
        assertFalse(validator.validate(param).isEmpty());
    }

    @Test
    void optionalTitleAllowsNullButNotBlank() {
        UploadNoteParam param = new UploadNoteParam();
        assertTrue(validator.validate(param).isEmpty());
        param.setTitle("   ");
        assertFalse(validator.validate(param).isEmpty());
        param.setTitle("Redis笔记");
        assertTrue(validator.validate(param).isEmpty());
    }

    @Test
    void userIdMustFitInALong() {
        UpdateUserStatusParam param = new UpdateUserStatusParam();
        param.setEnabled(true);
        param.setUserId("9223372036854775807");
        assertTrue(validator.validate(param).isEmpty());
        param.setUserId("9223372036854775808");
        assertFalse(validator.validate(param).isEmpty());
        param.setUserId("0");
        assertFalse(validator.validate(param).isEmpty());
    }

    private AskQuestionParam question() {
        AskQuestionParam param = new AskQuestionParam();
        param.setQuestion("查询我的笔记");
        return param;
    }
}
