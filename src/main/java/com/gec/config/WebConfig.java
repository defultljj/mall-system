package com.gec.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
 * Web MVC 配置类: 向 Spring MVC 注册 AuthInterceptor(登录拦截器)。
 *  - 只拦截业务 API(/User/** /Dept/** /Role/** /Brand/** /Category/** /AttrGroup/**
 *    /GoodsAttr/** /PublishGoods/** /Goods/**):
 *    这些接口操作的都要求先登录, 由拦截器统一挡在 Controller 之前。
 *  - 静态资源(/css /js /index.html 等)与 /Auth/** 不在此列表:
 *    登录/注册/登出/会话检查必须匿名可访问, 静态页面也不该被拦成 401
 *    (避免前端首屏加载时静态资源被拦截器误伤)。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;
    @Autowired
    private AdminWriteInterceptor adminWriteInterceptor;

    /* 注册拦截器并声明拦截路径(白名单之外的路径一律不过拦截器) */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                    "/User/**",
                    "/Dept/**",
                    "/Role/**",
                    "/Brand/**",
                    "/Category/**",
                    "/AttrGroup/**",
                    "/GoodsAttr/**",
                    "/PublishGoods/**",
                    //商品管理(9.12 补全): 列表/详情/级联删除同样要求先登录
                    "/Goods/**",
                    "/Order/**",
                    "/Inventory/**",
                    "/Activity/**",
                    "/Member/**",
                    "/Advertisement/**",
                    "/Analysis/**",
                    "/Stage2/**");
        registry.addInterceptor(adminWriteInterceptor)
                .addPathPatterns("/User/**", "/Dept/**", "/Role/**", "/Brand/**", "/Category/**",
                        "/AttrGroup/**", "/GoodsAttr/**", "/PublishGoods/**", "/Goods/**", "/Order/**",
                        "/Inventory/**", "/Activity/**", "/Member/**", "/Advertisement/**", "/Stage2/**");
    }
}
