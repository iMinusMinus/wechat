package weixin.mp.infrastructure.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;
import weixin.mp.application.MessageHandler;
import weixin.mp.domain.Context;
import weixin.mp.domain.ReplyMessage;
import weixin.mp.facade.MessageFacade;
import weixin.mp.facade.MessageFacadeImpl;
import weixin.mp.infrastructure.config.ManagementProperties;

import java.util.concurrent.CompletableFuture;

/**
 * 微信公众号需要访问域名下的服务，证实账号主体有开发者基本资质；同时，微信给该URL推送消息
 *
 * @author iMinusMinus
 * @date 2022-11-19
 */
@Controller
public class MessageController extends Tenant {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private ManagementProperties managementProperties;

    @Autowired
    @Qualifier("messageHandler")
    private MessageHandler messageHandler;

    /**
     * 验证消息的确来自微信服务器，同时给微信证明自身有接口服务能力
     *
     * @param id 多公众号场景下用于区分账号主体
     * @param signature 微信加密签名，signature结合了开发者填写的 token 参数和请求中的 timestamp 参数、nonce参数
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param echostr 随机字符串
     * @return 校验通过则返回请求时携带的随机字符串
     */
    @GetMapping(value = ExposedPath.MESSAGE, produces = {MediaType.TEXT_HTML_VALUE})
    @ResponseBody
    public Mono<String> challenge(@PathVariable("id") String id,
                                  @RequestParam("signature") String signature,
                                  @RequestParam("timestamp") String timestamp,
                                  @RequestParam("nonce") String nonce,
                                  @RequestParam("echostr") String echostr) {
        log.debug("/{}?signature={}&timestamp={}&nonce={}&echostr={}", id, signature, timestamp, nonce, echostr);
        Context ctx = discriminate(id, managementProperties.accounts());
        MessageFacade facade = new MessageFacadeImpl(ctx, messageHandler);
        return Mono.fromFuture(facade.challenge(signature, timestamp, nonce, echostr));
    }

    /**
     * 接受微信服务器推送的消息
     * <ul>
     *     注意事项：
     *     <li>微信服务器在五秒内收不到响应会断掉连接，并且重新发起请求，总共重试三次。
     *     假如服务器无法保证在五秒内处理并回复，可以直接回复空串（非xml中content为空）或success，微信服务器不会对此作任何处理，并且不会发起重试</li>
     *     <li>消息排重，推荐使用 msgid 排重，事件类型消息推荐使用FromUserName + CreateTime 排重</li>
     * </ul>
     *
     * @param id 多公众号场景下用于区分账号主体
     * @param signature 微信加密签名，signature结合了开发者填写的 token 参数和请求中的 timestamp 参数、nonce参数
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param openid 发消息给公众号的用户
     * @param encryptAlgorithm 加密算法（密文模式或混合模式才有）
     * @param messageSignature 消息签名（密文模式或混合模式才有）
     * @param xml
     * @return 确认接收消息或回复xml数据
     */
    @PostMapping(value = ExposedPath.MESSAGE)
    @ResponseBody
    public Mono<HttpEntity> onMessage(@PathVariable("id") String id,
                                      @RequestParam("signature") String signature,
                                      @RequestParam("timestamp") String timestamp,
                                      @RequestParam("nonce") String nonce,
                                      @RequestParam(value = "openid", required = false) String openid,
                                      @RequestParam(value = "encrypt_type", required = false) String encryptAlgorithm,
                                      @RequestParam(value = "msg_signature", required = false) String messageSignature,
                                      @RequestBody String xml) {
        log.debug("'/{}?signature={}&timestamp={}&nonce={}&openid={}&encrypt_type={}&msg_signature={}' receive msg: {}",
                id, signature, timestamp, nonce, openid, encryptAlgorithm, messageSignature, xml);
        Context ctx = discriminate(id, managementProperties.accounts());
        final HttpHeaders headers = new HttpHeaders();
        Boolean strict = null;
        if (ctx.strict() != null) {
            if (encryptAlgorithm == null || messageSignature == null) {
                log.error("message missing content, please check encrypt mode for appId={}", ctx.appId());
                headers.setContentType(MediaType.TEXT_PLAIN);
                HttpEntity entity = new HttpEntity<>(ReplyMessage.NO_RETRY.toXml(), headers);
                return Mono.just(entity);
            }
            assert ctx.key().length() == 43;
        }
        MessageFacade facade = new MessageFacadeImpl(ctx, messageHandler);

        CompletableFuture<MessageFacade.Response> response = facade.onMessage(signature, timestamp, nonce, openid,
                encryptAlgorithm, messageSignature, xml);

        return Mono.fromFuture(response).map(r -> {
            if (r.text() != null) {
                headers.setContentType(MediaType.TEXT_PLAIN);
                return new HttpEntity<>(r.text(), headers);
            } else {
                headers.setContentType(MediaType.TEXT_XML);
                return new HttpEntity<>(r.xml(), headers);
            }
        });
    }

}
