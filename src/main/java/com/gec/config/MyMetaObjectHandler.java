package com.gec.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/*
 * 元数据处理器: 插入时自动填 create_date / update_date, 更新时自动填 update_date。
 *
 * [Bug 修复] 资料里本类的 @Component 被注释掉, 改由 MybatisConfig 手动 new +
 *            GlobalConfig.setMetaObjectHandler() 注入 —— 这是 MyBatis-Plus 3.4.2 时代
 *            的全局配置写法; 在 3.5.x 中该方式已不再生效(且 GlobalConfig 手动装配
 *            也拿不到分页插件同源配置)。3.5+ 的自动配置会自动拾取 Spring 容器里的
 *            MetaObjectHandler Bean, 所以这里恢复 @Component, 交给 Spring 托管即可。
 *            MybatisConfig 中已同步删除手动 new + GlobalConfig 的代码。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject meta) {
        this.strictInsertFill( meta,
            "createDate", ()-> LocalDateTime.now(),
                LocalDateTime.class );
        this.strictInsertFill( meta,
            "updateDate", ()-> LocalDateTime.now(),
                LocalDateTime.class );
    }
    @Override
    public void updateFill(MetaObject meta) {
        this.strictUpdateFill( meta,
            "updateDate", ()-> LocalDateTime.now(),
                LocalDateTime.class);
    }
}