package weixin.mp.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weixin.mp.application.MessageHandler;
import weixin.mp.domain.ChallengeMessage;
import weixin.mp.domain.Context;
import weixin.mp.domain.MessageCorruptException;
import weixin.mp.domain.ReplyMessage;
import weixin.mp.domain.RequestMessageRetriever;
import weixin.mp.domain.ResponseMessageBuilder;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 微信公众号门面
 *
 * @author iMinusMinus
 * @date 2022-12-03
 */
public record MessageFacadeImpl(Context ctx, MessageHandler handler) implements MessageFacade {

    private static final Logger log = LoggerFactory.getLogger(MessageFacadeImpl.class);

    /**
     * 微信最多等待5秒，考虑网络因素，服务端设置为4.5秒
     */
    private static final int WAIT_MSG_TIMEOUT = 4500;

    @Override
    public CompletableFuture<String> challenge(String signature, String timestamp, String nonce, String echoStr) {
        ChallengeMessage challenge = new ChallengeMessage(ctx, signature, timestamp, nonce);
        boolean trustable = challenge.check();
        if (!trustable && ctx.strict() != null && ctx.strict()) {
            log.info("'{}' challenge fail", ctx.appId());
            return CompletableFuture.failedFuture(new MessageCorruptException("challenge message to " + ctx.appId() + " corrupt"));
        }
        return CompletableFuture.completedFuture(echoStr);
    }

    @Override
    public CompletableFuture<Response> onMessage(String signature, String timestamp, String nonce, String openid,
                                            String encryptAlgorithm, String messageSignature, String raw) {
        challenge(signature, timestamp, nonce, openid);
        if (ctx.strict() == null || !ctx.strict()) {
            log.trace("'{}' not in encrypt mode", ctx.appId());
        } else {
            Map<String, String> map = XmlParser.xmlToMap(raw);
            String encrypt = map.get(XmlParser.ENCRYPT_TAG);
            RequestMessageRetriever request = new RequestMessageRetriever(ctx, messageSignature, timestamp, nonce, encrypt);
            boolean trustable = request.check();
            if (!trustable) {
                log.info("'{}' message check fail", ctx.appId());
                return CompletableFuture.failedFuture(new MessageCorruptException("message/event to " + ctx.appId() + " corrupt"));
            }
            raw = request.retrieve(encryptAlgorithm);
            log.debug("decrypt message for '{}': {}", ctx.appId(), raw);
        }
        CompletableFuture<ReplyMessage> replyMessage = handler.handleMessage(ctx, XmlParser.parse(raw));
        ReplyMessage reply;
        try {
            reply = replyMessage.get(WAIT_MSG_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
        log.debug("handle '{}' message return result: {}", ctx.appId(), reply);
        ResponseMessageBuilder builder = new ResponseMessageBuilder(ctx, System.currentTimeMillis(), nonce, reply);
        Response response = (reply == ReplyMessage.ACK || reply == ReplyMessage.NO_RETRY) ?
                new Response(builder.build(), null) : new Response(null, builder.build());
        return CompletableFuture.completedFuture(response);
    }
}
