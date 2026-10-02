package com.campus.recommend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园学习资源推荐平台 - 启动类
 * 包扫描：com.campus.recommend
 * Mapper 扫描：com.campus.recommend.mapper
 */
@SpringBootApplication
@MapperScan("com.campus.recommend.mapper")
@EnableScheduling
public class CampusRecommendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusRecommendApplication.class, args);
    }
}
