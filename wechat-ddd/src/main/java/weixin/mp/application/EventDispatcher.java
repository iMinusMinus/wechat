package weixin.mp.application;

import weixin.mp.domain.Context;
import weixin.mp.domain.ReplyMessage;
import weixin.mp.domain.RequestMessage;

import java.util.concurrent.CompletableFuture;

public interface EventDispatcher extends MessageHandler {

    @Override
    default CompletableFuture<ReplyMessage> handleMessage(Context ctx, RequestMessage msg) {
        dispatch(ctx, msg);
        return null;
    }

    void dispatch(Context ctx, RequestMessage msg);
}
