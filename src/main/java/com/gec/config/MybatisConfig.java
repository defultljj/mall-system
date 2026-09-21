package com.gec.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration                 //1.设置为配置类
@EnableTransactionManagement   //2.启用事务管理。。
public class MybatisConfig {
    /*
     * [修复说明] 资料里本配置类还有一个 globalConfig() Bean, 手动 new
     * MyMetaObjectHandler 并塞进 GlobalConfig —— 3.4.x 老写法, 3.5.x 已失效,
     * 已删除; MyMetaObjectHandler 恢复 @Component 由 Spring 托管(3.5+ 自动拾取)。
     */
    //{1}分页插件。
    @Bean
    public MybatisPlusInterceptor mybatisIntercepter(){
        System.out.println("+--------------------[TRACE]--------------------+");
        System.out.println("{自动配置}创建了 MybatisPlus 拦截器..");
        System.out.println("+-----------------------------------------------+");
        MybatisPlusInterceptor it = new MybatisPlusInterceptor();
        //2. 设置分页拦截器。
        it.addInnerInterceptor(
            new PaginationInnerInterceptor(DbType.MYSQL)
        );
        return it;
    }
}