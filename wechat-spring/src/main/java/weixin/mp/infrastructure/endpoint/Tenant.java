package weixin.mp.infrastructure.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import weixin.mp.domain.Context;
import weixin.mp.infrastructure.cache.CacheName;
import weixin.mp.infrastructure.config.ManagementProperties;

import java.util.Map;
import java.util.function.Function;

public abstract class Tenant {

    @Autowired
    @Qualifier("tenantDiscriminator")
    protected Function<String, String> tenantDiscriminator;

    @Autowired
    protected CacheManager cacheManager;

    protected Context discriminate(String id, Map<String, ManagementProperties.Config> configMap) {
        String key = tenantDiscriminator.apply(id);
        ManagementProperties.Config cfg = configMap.get(key);
        return cacheManager.getCache(CacheName.LOCAL)
                .get(key, () -> new Context(cfg.accountId(), cfg.appId(), cfg.appSecret(), cfg.token(), cfg.key(), cfg.verify()));
    }
}
