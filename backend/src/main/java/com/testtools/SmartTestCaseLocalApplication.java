package com.testtools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartTestCaseLocalApplication {
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("      智能测试用例选择工具 - 本地版本");
        System.out.println("==============================================");
        System.out.println("正在启动服务...");
        
        SpringApplication.run(SmartTestCaseLocalApplication.class, args);
        
        System.out.println("==============================================");
        System.out.println("服务启动成功！");
        System.out.println("后端服务地址: http://localhost:8080");
        System.out.println("数据库控制台: http://localhost:8080/h2-console");
        System.out.println("==============================================");
    }
}