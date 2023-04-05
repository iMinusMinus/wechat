package weixin.mp.infrastructure.cache;

public interface CacheName {

    /**
     * 仅缓存在本地
     */
    String LOCAL = "local";

    /**
     * 仅分布式缓存
     */
    String DISTRIBUTABLE = "remote";

    /**
     * 缓存微信access_token，可远近端缓存组合
     */
    String ACCESS_TOKEN = "accessToken";
}
