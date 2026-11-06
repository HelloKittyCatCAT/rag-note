package com.sya.ragnote.service.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.dao.auth.InvitationDao;
import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.entity.auth.Invitation;
import com.sya.ragnote.entity.user.User;
import com.sya.ragnote.enums.result.AuthResultEnum;
import com.sya.ragnote.enums.user.UserRole;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.service.impl.auth.RegistrationServiceImpl;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

class RegistrationServiceTest {
    final UserDao users = mock(UserDao.class);
    final InvitationDao invitations = mock(InvitationDao.class);
    final RegistrationService service = new RegistrationServiceImpl(invitations, users);

    Invitation invitation() {
        Invitation i = new Invitation();
        i.setId(UUID.randomUUID());
        i.setExpiresAt(Instant.now().plusSeconds(3600));
        return i;
    }

    @Test
    void expiredInvitationDoesNotCreateAccount() {
        Invitation i = invitation();
        i.setExpiresAt(Instant.now().minusSeconds(1));
        when(invitations.findByHashForUpdate("hash")).thenReturn(i);
        assertEquals(
                AuthResultEnum.REGISTRATION_FAILED,
                assertThrows(
                                ServiceSystemException.class,
                                () -> service.createUser("alice", "encoded", "hash"))
                        .getResultCode());
        verifyNoInteractions(users);
    }

    @Test
    void consumedInvitationDoesNotCreateAccount() {
        Invitation i = invitation();
        i.setUsedBy(2L);
        when(invitations.findByHashForUpdate("hash")).thenReturn(i);
        assertThrows(
                ServiceSystemException.class, () -> service.createUser("alice", "encoded", "hash"));
        verifyNoInteractions(users);
    }

    @Test
    void registrationAlwaysCreatesUserAndConsumesInvitation() {
        Invitation i = invitation();
        when(invitations.findByHashForUpdate("hash")).thenReturn(i);
        doAnswer(
                        a -> {
                            a.getArgument(0, User.class).setId(11L);
                            return null;
                        })
                .when(users)
                .insert(any());
        when(invitations.markUsed(i.getId(), 11)).thenReturn(1);
        User user = service.createUser("alice", "encoded", "hash");
        assertEquals(UserRole.USER, user.getRole());
        assertTrue(user.isEnabled());
        verify(invitations).markUsed(i.getId(), 11);
    }

    @Test
    void failedAtomicConsumePropagatesFailureForTransactionRollback() {
        Invitation i = invitation();
        when(invitations.findByHashForUpdate("hash")).thenReturn(i);
        doAnswer(
                        a -> {
                            a.getArgument(0, User.class).setId(11L);
                            return null;
                        })
                .when(users)
                .insert(any());
        assertEquals(
                AuthResultEnum.REGISTRATION_FAILED,
                assertThrows(
                                ServiceSystemException.class,
                                () -> service.createUser("alice", "encoded", "hash"))
                        .getResultCode());
    }
}
