package weixin.mp.infrastructure.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 单redis分布式锁
 */
public class RedisBasedDistributableLock implements Lock {

    private static final Logger log = LoggerFactory.getLogger(RedisBasedDistributableLock.class);

    private final ReactiveStringRedisTemplate stringRedisTemplate;

    private final String key;

    /**
     * key超时时间
     */
    private final Duration expiration;

    private static final String machineId = System.getProperty("container.name", System.getenv("CONTAINER_NAME")); // TODO

    public RedisBasedDistributableLock(ReactiveStringRedisTemplate stringRedisTemplate, String key, Duration expiration) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.key = key;
        this.expiration = expiration;
    }

    @Override
    public void lock() {
        boolean locked = false;
        while (!locked) {
            locked = tryLock();
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        lock();
    }

    @Override
    public boolean tryLock() {
        return stringRedisTemplate.opsForValue()
                .setIfAbsent(key, Thread.currentThread().getName() + "@" + machineId, expiration).block();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        long start = System.currentTimeMillis();
        boolean locked = false;
        while (!locked && System.currentTimeMillis() - start < unit.toMillis(time)) {
            locked = tryLock();
        }
        return locked;
    }

    @Override
    public void unlock() {
        String lockOwner = stringRedisTemplate.opsForValue().get(key).block();
        if (lockOwner == null) {
            log.info("key[{}] already expired!", key);
            return;
        }
        String releaser = Thread.currentThread().getName() + "@" + machineId;
        if (!lockOwner.equals(releaser)) {
            log.error("unlock action must be lock owner execute: owner={}, releaser={}", lockOwner, releaser);
            return;
        }
        stringRedisTemplate.delete(key);
    }

    @Override
    public Condition newCondition() {
        return null; // TODO, may be publish event, so waiter can get cache
    }

}
