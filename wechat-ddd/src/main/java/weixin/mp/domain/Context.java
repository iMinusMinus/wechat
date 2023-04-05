package weixin.mp.domain;

/**
 * @param weixinAccount 微信公众号
 * @param appId 开发者ID
 * @param appSecret 开发者密码
 * @param token 令牌
 * @param key 消息加解密密钥
 * @param strict null, 明文模式; false, 校验但不阻断; true, 未通过校验则不继续处理
 */
public record Context(String weixinAccount, String appId, String appSecret, String token, String key, Boolean strict) {
}
