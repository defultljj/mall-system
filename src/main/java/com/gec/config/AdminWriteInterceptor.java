package com.gec.config;

import com.gec.domain.vo.LoginUserVO;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Course-project permission boundary: business reads need login; mutations need the administrator role. */
@Component
public class AdminWriteInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("GET".equalsIgnoreCase(request.getMethod())) return true;
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("loginUser");
        if (value instanceof LoginUserVO && Integer.valueOf(1).equals(((LoginUserVO) value).getRoleId())) return true;
        response.setStatus(403);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write("{\"result\":\"failed\",\"cause\":\"当前角色只有查看权限\"}");
        return false;
    }
}
