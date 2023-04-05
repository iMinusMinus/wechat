package weixin.mp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "weixin")
public record ManagementProperties(Map<String/* id */, Config> accounts) {

    /**
     * @param accountId 微信公众号账号
     * @param appId 微信生成的开发者ID
     * @param appSecret 开发者密码
     * @param token 验证服务器时配置的令牌
     * @param verify true, 校验且失败抛异常; null, 校验仅error提示; false, 跳过校验
     * @param mode 消息加密方式
     * @param key 加解密需要的密钥
     */
    public record Config(String accountId, String appId, String appSecret, String token, Boolean verify, Mode mode, String key) {}

    public enum Mode {
        PLAINTEXT,
        MIXTURE,
        CIPHERTEXT,
        ;
    }
}
