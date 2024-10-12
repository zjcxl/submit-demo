package com.demo.submit.demo.util;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
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

    private final Map<String, String> CACHE_MAP = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> CACHE_MAP_2 = new ConcurrentHashMap<>();
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void test() {
        test1();
        test2();
        test3();
    }

    public void write(String key) {
        log.info("write key: {}", key);
    }

    @Test
    public void test1() {
        System.out.println("--------------test1--------------");
        run(uuid -> {
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(uuid))) {
                // 设置值
                stringRedisTemplate.opsForValue().setIfAbsent(uuid, "");
                // 进行输出操作
                write(uuid);
            }
        });
    }

    @Test
    public void test2() {
        System.out.println("--------------test2--------------");
        run(uuid -> {
            if (!CACHE_MAP.containsKey(uuid)) {
                // 设置值
                CACHE_MAP.put(uuid, "");
                // 进行输出操作
                write(uuid);
            }
        });
    }

    @Test
    public void test3() {
        System.out.println("--------------test3--------------");
        run(uuid -> {
            var count = CACHE_MAP_2.computeIfAbsent(uuid, k -> new AtomicInteger()).incrementAndGet();
            if (count == 1) {
                // 进行输出操作
                write(uuid);
            }
        });
    }

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

}
