package com.sya.ragnote.contract;

import static org.junit.jupiter.api.Assertions.*;

import com.sya.ragnote.enums.result.*;
import com.sya.ragnote.param.auth.LoginParam;
import com.sya.ragnote.param.auth.RegisterParam;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ContractInvariantTest {
    @Test
    void businessErrorCodesAreGloballyUnique() {
        List<ResultCode[]> groups =
                List.of(
                        AuthResultEnum.values(),
                        NoteResultEnum.values(),
                        ChatResultEnum.values(),
                        UserResultEnum.values(),
                        CommonResultEnum.values());
        Set<Integer> seen = new HashSet<>();
        for (ResultCode[] group : groups) {
            for (ResultCode code : group) {
                assertTrue(seen.add(code.getCode()), "错误码重复：" + code.getCode());
                assertFalse(code.getMsg().isBlank());
            }
        }
    }

    @Test
    void unknownCodeHasNoAccidentalMessage() {
        assertEquals("", AuthResultEnum.getMsg(-1));
        assertEquals("", NoteResultEnum.getMsg(-1));
        assertEquals("", ChatResultEnum.getMsg(-1));
        assertEquals("", UserResultEnum.getMsg(-1));
        assertEquals("", CommonResultEnum.getMsg(-1));
    }

    @Test
    void credentialsAreExcludedFromGeneratedToString() {
        LoginParam login = new LoginParam();
        login.setPassword("private-password");
        RegisterParam registration = new RegisterParam();
        registration.setPassword("private-password");
        registration.setInviteToken("private-invitation");
        assertFalse(login.toString().contains("private-password"));
        assertFalse(registration.toString().contains("private-password"));
        assertFalse(registration.toString().contains("private-invitation"));
    }
}
