package com.campus.recommend.config;

import com.campus.recommend.entity.Admin;
import com.campus.recommend.mapper.AdminMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动初始化：若管理员表为空，则创建默认管理员（username=admin / password=admin123）。
 * 便于系统开箱即可登录管理端；生产环境请务必修改密码。
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Long cnt = adminMapper.selectCount(null);
        if (cnt != null && cnt == 0) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("系统管理员");
            admin.setRole(1);
            admin.setStatus(1);
            admin.setCreateTime(LocalDateTime.now());
            admin.setUpdateTime(LocalDateTime.now());
            adminMapper.insert(admin);
            log.info("已创建默认管理员账号：admin / admin123（请及时修改密码）");
        }
    }
}
