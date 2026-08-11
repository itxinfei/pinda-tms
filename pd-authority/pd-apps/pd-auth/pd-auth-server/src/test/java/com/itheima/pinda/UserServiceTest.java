package com.itheima.pinda;

import com.itheima.pinda.authority.biz.service.auth.UserService;
import org.junit.Assert;
import org.junit.Test;

/**
 * UserService 接口契约测试
 *
 * <p>修复说明：原测试类为空壳（无 @Test 方法），且注入了 JPA 专属的
 * {@code TestEntityManager}（本项目使用 MyBatis-Plus，无 JPA 依赖），
 * 导致 surefire 报 "No runnable methods" / 依赖注入失败。
 * 改为不依赖 Spring 上下文的纯 JUnit 测试，验证接口契约存在。</p>
 */
public class UserServiceTest {

    /**
     * 校验 UserService 接口可加载，且继承自 MyBatis-Plus IService 基类
     */
    @Test
    public void userServiceInterfaceContract() {
        Assert.assertNotNull("UserService 接口应存在", UserService.class);
        // 接口应继承 MyBatis-Plus IService，保证具备通用 CRUD 能力
        Assert.assertTrue("UserService 应继承 IService",
            com.baomidou.mybatisplus.extension.service.IService.class.isAssignableFrom(UserService.class));
    }
}
