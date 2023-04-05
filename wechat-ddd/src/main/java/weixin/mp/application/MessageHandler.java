package weixin.mp.application;

import weixin.mp.domain.Context;
import weixin.mp.domain.ReplyMessage;
import weixin.mp.domain.RequestMessage;

import java.util.concurrent.CompletableFuture;

public interface MessageHandler {

    CompletableFuture<ReplyMessage> handleMessage(Context ctx, RequestMessage msg);

}
