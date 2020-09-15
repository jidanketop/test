package com.example.demo;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @author kelly
 * @create 2020/9/15 5:51 下午
 */
@RestController
public class DemoController {

    private final RedisTemplate<Object, Object> redisTemplate;
    /**
     * 单机全局限流器(限制每秒为1000)
     */
    private static final RateLimiter rateLimiter = RateLimiter.create(1000);

    public DemoController(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("demo")
    public Object demo(){
        return "Demo";
    }


    /**
     * Redis 限流
     * @return
     */
    @GetMapping("redis/limit")
    public Object redisLimit(){
        String key = "limitCount";
        int limit = 1000;
        if (redisTemplate.hasKey(key)) {
            Long count = (Long) redisTemplate.opsForValue().get(key);
            if (count > limit){
                return "当前人数较多，请稍后再试一下。";
            }
        }
        redisTemplate.opsForValue().set(key,1L,1L, TimeUnit.SECONDS);
        // ……

        // ……
        return "success";
    }

    /**
     * RateLimiter 限流
     * @return
     */
    @GetMapping("guava/limit")
    public Object rateLimiter(){
        if (!rateLimiter.tryAcquire()) {
            System.out.println("限流中......");
            return "当前人数较多，请稍后再试一下。";
        }
        // ……

        // ……
        return "success";
    }
}
