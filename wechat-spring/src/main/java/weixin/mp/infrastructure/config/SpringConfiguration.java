package weixin.mp.infrastructure.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import weixin.mp.application.CompositeMessageHandler;
import weixin.mp.application.EventDispatcher;
import weixin.mp.application.MessageDispatcher;
import weixin.mp.application.MessageHandler;
import weixin.mp.domain.Context;
import weixin.mp.domain.ReplyMessage;
import weixin.mp.domain.RequestMessage;
import weixin.mp.infrastructure.cache.CacheKey;
import weixin.mp.infrastructure.cache.LockKey;
import weixin.mp.infrastructure.exceptions.RetryableException;
import weixin.mp.infrastructure.lock.RedisBasedDistributableLock;
import weixin.mp.infrastructure.rpc.OpenAI;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 公众号、小程序运行所需应用配置
 *
 * @author iMinusMinus
 * @date 2022-11-19
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(value = {ManagementProperties.class})
@EnableCaching
public class SpringConfiguration {

    @Value("${openai.bearerToken}")
    private String bearerToken;

    private static final int WEIXIN_ACCESS_TOKEN_TTL = 7200 - 5 * 60;

    private static final int WEIXIN_REPLY_CACHE_EXPIRATION = 3 * 5;

    @Bean
    public Function<String, String> tenantDiscriminator(ManagementProperties managementProperties) {
        return Function.identity(); // XXX 使用配置或数据库做映射（注意保证唯一性）
    }

    @Bean
    public OpenAI chatGPT() {
        return new OpenAI.JavaClient(bearerToken);
    }

    @Bean("xlock")
    @ConditionalOnBean(value = {ReactiveStringRedisTemplate.class})
    public Lock xlock(ReactiveStringRedisTemplate stringRedisTemplate) {
        return new RedisBasedDistributableLock(stringRedisTemplate, LockKey.TOKEN_LOCK_FMT, Duration.ofSeconds(WEIXIN_ACCESS_TOKEN_TTL)); // FIXME
    }

    @Bean("xlock")
    @ConditionalOnMissingBean(value = {ReactiveStringRedisTemplate.class})
    public Lock accessTokenLock() {
        return new ReentrantLock();
    }


    @Bean
    public MessageHandler messageHandler(ObjectProvider<EventDispatcher> eventDispatchers, ObjectProvider<MessageDispatcher> messageDispatchers) {
        List<MessageHandler> delegates = new ArrayList<>();
        delegates.addAll(eventDispatchers.stream().toList());
        delegates.addAll(messageDispatchers.stream().toList());
        return new CompositeMessageHandler(delegates);
    }

    @Bean
    @ConditionalOnBean(value = {ReactiveRedisTemplate.class})
    public MessageDispatcher messageDispatcher(ReactiveRedisTemplate redisTemplate) {
        return new MessageDispatcher() {
            @Override
            public CompletableFuture<ReplyMessage> handleMessage(Context ctx, RequestMessage msg) {
                String key = CacheKey.REPLY_MSG_FMT.formatted(ctx.appId(), msg.msgId());
//                redisTemplate.listenToPattern(CacheKey.CHANNEL_REPLY_MSG)
                return redisTemplate.opsForValue()
                        .getAndExpire(key, Duration.ofSeconds(WEIXIN_REPLY_CACHE_EXPIRATION))
                        .flatMap(x -> x == null ?
                                Mono.error(new RetryableException("get null from redis for key: " + key)) :
                                x instanceof ReplyMessage ? Mono.just(x) : Mono.error(new RuntimeException("incompatible object type: " + x.getClass())))
                        .toFuture();
            }
        };
    }

}
