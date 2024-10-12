package com.demo.submit.demo.util;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * SubmitAopUtilTests
 *
 * @author chenxueli
 * @date 2024-10-12 19:45:00
 */
@Slf4j
@SpringBootTest
public class SubmitAopUtilTests {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void test() {
        test1();
        test2();
        test3();
    }

    /**
     * 方法接收一个消费函数，已处理10轮，每轮10次（相同key）的高并发请求
     *
     * @param consumer 具体的处理函数，主要用于模拟是否能够有效拦截重复请求
     */
    public void run(Consumer<String> consumer) {
        var service = Executors.newVirtualThreadPerTaskExecutor();
        for (var i = 0; i < 10; i++) {
            var uuid = UUID.randomUUID().toString();
            Stream.iterate(0, n -> n + 1).limit(10).forEach(j -> {
                service.submit(() -> consumer.accept(uuid));
            });
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        service.close();
    }

    /**
     * 模拟的处理业务逻辑，这里只是简单的输出。如果每个key只输出了一次说明这个方法是合理的
     *
     * @param key key
     */
    public void write(String key) {
        log.info("write key: {}", key);
    }

    /**
     * 使用redis作为缓存，进行重复请求拦截
     * 先判断key是否存在，不存在则设置key，然后进行输出操作
     */
    @Test
    public void test1() {
        System.out.println("--------------test1--------------");
        run(uuid -> {
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(uuid))) {
                stringRedisTemplate.opsForValue().setIfAbsent(uuid, "");
                write(uuid);
            }
        });
    }

    /**
     * 使用ConcurrentHashMap作为缓存，进行重复请求拦截
     * 先判断key是否存在，不存在则设置key，然后进行输出操作
     */
    @Test
    public void test2() {
        System.out.println("--------------test2--------------");
        var cacheMap = new ConcurrentHashMap<String, String>();
        run(uuid -> {
            if (!cacheMap.containsKey(uuid)) {
                // 设置值
                cacheMap.put(uuid, "");
                // 进行输出操作
                write(uuid);
            }
        });
    }

    /**
     * 使用ConcurrentHashMap作为缓存，进行重复请求拦截
     * 使用computeIfAbsent方法，先判断key是否存在，不存在则设置一个原子计数器，然后加1并且获取值，如果值为1则进行输出操作
     */
    @Test
    public void test3() {
        System.out.println("--------------test3--------------");
        var cacheMap = new ConcurrentHashMap<String, AtomicInteger>();
        run(uuid -> {
            var count = cacheMap.computeIfAbsent(uuid, k -> new AtomicInteger()).incrementAndGet();
            if (count == 1) {
                // 进行输出操作
                write(uuid);
            }
        });
    }

}
