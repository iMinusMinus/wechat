package weixin.mp.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weixin.mp.domain.Context;
import weixin.mp.domain.ReplyMessage;
import weixin.mp.domain.RequestMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CompositeMessageHandler implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(CompositeMessageHandler.class);

    private final List<MessageHandler> delegates;

    public CompositeMessageHandler(List<MessageHandler> delegates) {
        this.delegates = delegates;
    }

    @Override
    public CompletableFuture<ReplyMessage> handleMessage(Context ctx, RequestMessage msg) {
        for (MessageHandler delegate : delegates) {
            CompletableFuture<ReplyMessage> reply = delegate.handleMessage(ctx, msg);
            if (reply != null && reply.isDone()) {
                return reply;
            }
        }
        log.warn("No suitable MessageHandler match [{}]'s request message", ctx.appId());
        return CompletableFuture.completedFuture(ReplyMessage.ACK);
    }
}
