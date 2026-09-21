package com.gec;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Date;


/*
*   1.把光标定位到需要导入包的类名
*   2.ALT + ENTER ==>【弹出列表】==>【选择导入的包】
*/

@SpringBootApplication
@EnableScheduling
@MapperScan(
        basePackages={"com.gec.dao"}
)
public class MallAPP {
    public static void main(String[] args) {
        /* 1.启动　SpringBoot 主程序. */
        SpringApplication.run(MallAPP.class, args);
        System.out.println("+----------------------------------+");
        System.out.println("Spring Boot is RUNNING..");
        System.out.println("TIME:"+ (new Date()).toLocaleString());
        System.out.println("+----------------------------------+");
    }
}
