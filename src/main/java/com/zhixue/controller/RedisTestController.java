package com.zhixue.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
public class RedisTestController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/test")
    public String test() {
        // 设置键值对到 Redis
        redisTemplate.opsForValue().set("name", "zhixue");

        // 从 Redis 获取值并返回
        return (String) redisTemplate.opsForValue().get("name");
    }
}
