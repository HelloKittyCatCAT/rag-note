package com.sya.ragnote.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.dao.auth.InvitationDao;
import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.entity.user.User;
import com.sya.ragnote.enums.result.*;
import com.sya.ragnote.enums.user.UserRole;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.param.user.UpdateUserStatusParam;
import com.sya.ragnote.service.impl.user.UserServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {
    final UserDao users = mock(UserDao.class);
    final InvitationDao invitations = mock(InvitationDao.class);
    final UserService service =
            new UserServiceImpl(users, invitations, mock(PasswordEncoder.class));

    User admin(long id) {
        User user = new User();
        user.setId(id);
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        return user;
    }

    UpdateUserStatusParam disable(long id) {
        var p = new UpdateUserStatusParam();
        p.setUserId(Long.toString(id));
        p.setEnabled(false);
        return p;
    }

    @Test
    void cannotDisableSelf() {
        when(users.findById(1)).thenReturn(admin(1));
        when(users.lockById(1)).thenReturn(admin(1));
        assertEquals(
                UserResultEnum.ADMIN_DISABLE_NOT_ALLOWED,
                assertThrows(
                                ServiceSystemException.class,
                                () -> service.updateStatus(1, disable(1)))
                        .getResultCode());
        verify(users, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void disablingRevokesSessionsInSameBusinessOperation() {
        when(users.findById(1)).thenReturn(admin(1));
        User target = new User();
        target.setId(2L);
        target.setRole(UserRole.USER);
        target.setEnabled(true);
        when(users.lockById(2)).thenReturn(target);
        service.updateStatus(1, disable(2));
        var order = inOrder(users);
        order.verify(users).lockAdminChanges();
        order.verify(users).findById(1);
        order.verify(users).lockById(2);
        order.verify(users).updateEnabled(2, false);
        order.verify(users).revokeSessions(2);
    }

    @Test
    void paginationOffsetDoesNotOverflowIntegerRange() {
        when(users.findById(1)).thenReturn(admin(1));
        var p = new com.sya.ragnote.param.user.QueryUserParam();
        p.setPage(Integer.MAX_VALUE);
        p.setSize(100);
        when(users.listUsers(214748364600L, 100)).thenReturn(java.util.List.of());
        service.listUsers(1, p);
        verify(users).listUsers(214748364600L, 100);
    }

    @Test
    void existingAdminDoesNotNeedBootstrapCredentialsOrChangePassword() {
        when(users.countAdmins()).thenReturn(1L);
        service.initializeAdmin(null, null);
        verify(users, never()).insert(any());
    }

    @Test
    void firstAdminRequiresValidEnvironmentCredentials() {
        assertThrows(IllegalStateException.class, () -> service.initializeAdmin(null, null));
        verify(users, never()).insert(any());
    }
}
