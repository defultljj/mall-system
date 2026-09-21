package com.gec;

import com.gec.config.AdminWriteInterceptor;
import com.gec.domain.vo.LoginUserVO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminWriteInterceptorTest {
    private final AdminWriteInterceptor interceptor = new AdminWriteInterceptor();

    @Test
    void readsAreAllowedForLoggedInRoles() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/Inventory/list");
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void nonAdminCannotMutateBusinessData() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Inventory/adjust");
        LoginUserVO user = new LoginUserVO(); user.setRoleId(5);
        request.getSession().setAttribute("loginUser", user);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }

    @Test
    void administratorCanMutateBusinessData() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/Inventory/adjust");
        LoginUserVO user = new LoginUserVO(); user.setRoleId(1);
        request.getSession().setAttribute("loginUser", user);
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
