package com.example.user;

import org.junit.Test;
import org.junit.Assert;

/**
 * 用户服务测试类
 */
public class UserServiceTest {
    
    /**
     * 测试用户登录功能
     */
    @Test
    public void testUserLogin() {
        // 测试正常登录
        UserService userService = new UserService();
        boolean result = userService.login("admin", "password123");
        Assert.assertTrue("用户登录应该成功", result);
    }
    
    /**
     * 测试用户注册功能
     */
    @Test
    public void testUserRegister() {
        // 测试新用户注册
        UserService userService = new UserService();
        User newUser = new User("newuser", "newuser@example.com", "password123");
        boolean result = userService.register(newUser);
        Assert.assertTrue("用户注册应该成功", result);
    }
    
    /**
     * 测试密码验证功能
     */
    @Test
    public void testPasswordValidation() {
        // 测试密码强度验证
        UserService userService = new UserService();
        Assert.assertTrue("强密码应该通过验证", userService.validatePassword("StrongP@ssw0rd"));
        Assert.assertFalse("弱密码应该验证失败", userService.validatePassword("123"));
    }
    
    /**
     * 测试用户资料更新
     */
    @Test
    public void testUserProfileUpdate() {
        // 测试用户资料更新
        UserService userService = new UserService();
        User user = userService.getUser("admin");
        user.setEmail("newemail@example.com");
        boolean result = userService.updateProfile(user);
        Assert.assertTrue("用户资料更新应该成功", result);
    }
    
    /**
     * 测试用户权限验证
     */
    @Test
    public void testUserPermission() {
        // 测试用户权限检查
        UserService userService = new UserService();
        Assert.assertTrue("管理员应该有管理权限", userService.hasPermission("admin", "MANAGE_USERS"));
        Assert.assertFalse("普通用户不应该有管理权限", userService.hasPermission("user", "MANAGE_USERS"));
    }
}