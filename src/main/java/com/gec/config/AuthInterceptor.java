package com.gec.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/*
 * 登录拦截器(会话校验层, 不依赖 controller 包):
 *  - preHandle 检查 session 里有没有 loginUser(/Auth/login 成功时写入的 LoginUserVO);
 *    有 → 放行(true), 没有 → 401 + JSON 提示并拦截(false)。
 *  - 它只管"有没有登录"; 业务校验(账号密码错/账号已存在/参数缺省)抛出的异常
 *    由各 Controller 自带的 @ExceptionHandler 统一处理, 两件事互不混淆。
 *  - 注册在 config.WebConfig 中, 只拦 /User/** /Dept/** /Role/** /Brand/** /Category/**
 *    /AttrGroup/** /GoodsAttr/** /PublishGoods/** 业务路径, 静态资源与 /Auth/** 登录注册接口天然不拦。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /*
     * 每个业务请求到达 Controller 前都会先经过这里。
     * session 存在且已登录 → 放行; 否则写 401 JSON 后不放行。
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        //1.取当前会话(false: 没有就返回 null, 不主动创建新 session)
        HttpSession session = request.getSession(false);
        Object loginUser = (session == null) ? null
                : session.getAttribute("loginUser");
        //2.已登录 → 放行
        if (loginUser != null) {
            return true;
        }
        //3.未登录 → 写 401 + JSON(前端 axios 拦截到非 200 可据此跳登录页)
        //  不 import R(配置包不反向依赖 controller 包), 直接手写最小 JSON
        response.setStatus(401);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(
            "{\"result\":\"failed\",\"cause\":\"未登录或登录已过期\"}");
        return false;
    }
}
