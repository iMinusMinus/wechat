package weixin.mp.facade;

import jakarta.validation.constraints.NotEmpty;

import java.util.concurrent.CompletableFuture;

/**
 * 微信认证公众号服务器，推送消息/事件
 *
 * @author iMinusMinus
 * @date 2022-12-03
 */
public interface MessageFacade {

    /**
     * 验证消息的确来自微信服务器，同时给微信证明自身有接口服务能力
     *
     * @param signature 微信加密签名，signature结合了开发者填写的 token 参数和请求中的 timestamp 参数、nonce参数
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param echoStr 随机字符串
     * @return 签名校验成功则返回echoStr
     */
    CompletableFuture<String> challenge(@NotEmpty  String signature, @NotEmpty String timestamp, @NotEmpty String nonce, @NotEmpty String echoStr);

    /**
     * 接受微信服务器推送的消息
     * <ul>
     *     注意事项：
     *     <li>微信服务器在五秒内收不到响应会断掉连接，并且重新发起请求，总共重试三次。
     *     假如服务器无法保证在五秒内处理并回复，可以直接回复空串（非xml中content为空）或success，微信服务器不会对此作任何处理，并且不会发起重试</li>
     *     <li>消息排重，推荐使用 msgid 排重，事件类型消息推荐使用FromUserName + CreateTime 排重</li>
     * </ul>
     *
     * @param signature 微信加密签名，signature结合了开发者填写的 token 参数和请求中的 timestamp 参数、nonce参数
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param openid 发消息给公众号的用户
     * @param encryptAlgorithm 加密算法（密文模式或混合模式才有）
     * @param messageSignature 消息签名（密文模式或混合模式才有）
     * @param raw xml加密内容及其他信息
     * @return 确认接收消息或回复xml数据
     */
    CompletableFuture<Response> onMessage(@NotEmpty String signature, @NotEmpty String timestamp, @NotEmpty String nonce, String openid,
                                     String encryptAlgorithm, String messageSignature, @NotEmpty String raw);

    /**
     * 收到消息的回复。如果只是确认，则取text，否则取xml
     */
    record Response(String text, String xml) {}
}
