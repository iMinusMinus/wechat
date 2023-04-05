package weixin.mp.infrastructure.cache;

/**
 * 缓存key：长度不宜太短，但最好不要超过1024byte。组成key的各部分有从属关系的，推荐用"."分隔（或":"）；多个单词组成统一部分的，单词间推荐用"_"（或"-"）。
 */
public interface CacheKey {

    String TOKEN_FMT = "cache:weixin.mp.access_token.%1$s";

    String CHANNEL_REPLY_MSG = "weixin.mp.reply.*";

    String REPLY_MSG_FMT = "weixin.mp.reply.%1$s.%2$s";

}
