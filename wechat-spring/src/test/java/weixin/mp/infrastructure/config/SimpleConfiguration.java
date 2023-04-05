package weixin.mp.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import weixin.mp.application.EventDispatcher;
import weixin.mp.application.MessageDispatcher;
import weixin.mp.domain.Context;
import weixin.mp.domain.ReplyMessage;
import weixin.mp.domain.RequestMessage;
import weixin.mp.infrastructure.rpc.OpenAI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@TestConfiguration(proxyBeanMethods = false)
public class SimpleConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SimpleConfiguration.class);

    private static final String CHAT_GPT_PROMPT = "/ChatGPT: ";

    private static final String[] CONVERSATION_STOP = {"Q:", "A:"};

    private static final int MAX_TOKEN = 2048;

    private final Map<String, List<String>> conversationContext = new WeakHashMap<>();

    private final Map<String, Integer> rateLimiter = new ConcurrentHashMap<>();

    @Bean
    public EventDispatcher eventDispatcher() {
        return (ctx, msg) -> {

        };
    }

    @Bean
    public MessageDispatcher messageDispatcher(OpenAI chatGPT) {
        return new MessageDispatcher() {
            @Override
            public CompletableFuture<ReplyMessage> handleMessage(Context ctx, RequestMessage msg) {
                ReplyMessage reply;
                switch (msg.msgType()) {
                    case TEXT -> {
                        String q = ((RequestMessage.TextMessage) msg).content();
                        String a = q;
                        Integer times = rateLimiter.get(msg.fromOpenId());
                        if (q.startsWith(CHAT_GPT_PROMPT) && (times == null || times < 10) && q.length() < MAX_TOKEN / 2) {
                            StringBuilder prompt = new StringBuilder(q.length());
                            q = q.substring(CHAT_GPT_PROMPT.length());
                            List<String> history = conversationContext.get(msg.fromOpenId());
                            if (history == null) {
                                history = new ArrayList<>();
                            }
                            int offset = history.size() > 3 ? history.size() - 3 : 0;
                            for (; offset < history.size(); offset++) {
                                prompt.append(offset % 2 == 0 ? CONVERSATION_STOP[0] : CONVERSATION_STOP[1]).append(history.get(offset));
                                prompt.append("\n");
                            }
                            if (offset != 0) {
                                prompt.append(CONVERSATION_STOP[0]);
                            }
                            prompt.append(q).append("\n");
                            if (offset != 0) {
                                prompt.append(CONVERSATION_STOP[1]).append("\n");
                            }
                            if (prompt.length() > MAX_TOKEN / 2) {
                                log.info("[{}]'s conversation content too long, discard history", msg.fromOpenId());
                                prompt.delete(0, prompt.length());
                                prompt.append(q);
                            }
                            int max_tokens = Math.max(prompt.length() * 2, 256);
                            String model = "text-davinci-003"; // text-davinci-003 max tokens: 4000, other model max tokens: 2000
                            OpenAI.CompletionRequest request = OpenAI.CompletionRequest.of(model, prompt.toString(), max_tokens, offset != 0 ? CONVERSATION_STOP : null);
                            OpenAI.TextResponse textResponse;
                            rateLimiter.put(msg.fromOpenId(), times != null ? times + 1: 1);
                            try {
                                textResponse = chatGPT.complete(request).get(5, TimeUnit.SECONDS);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            a = textResponse.choices().get(0).text();
                            history.add(prompt.toString());
                            history.add(a);
                            conversationContext.put(msg.fromOpenId(), history);
                        }
                        reply = new ReplyMessage.TextMessage(msg.fromOpenId(), msg.toAccount(),
                                System.currentTimeMillis(), a);
                    }
                    case IMAGE -> reply = new ReplyMessage.ImageMessage(msg.fromOpenId(), msg.toAccount(),
                            System.currentTimeMillis(), ((RequestMessage.ImageMessage) msg).mediaId());
                    case VOICE -> reply = new ReplyMessage.VoiceMessage(msg.fromOpenId(), msg.toAccount(),
                            System.currentTimeMillis(), ((RequestMessage.VoiceMessage) msg).mediaId());
                    case VIDEO -> reply = new ReplyMessage.VideoMessage(msg.fromOpenId(), msg.toAccount(),
                            System.currentTimeMillis(), ((RequestMessage.VideoMessage) msg).mediaId(),
                            ((RequestMessage.VideoMessage) msg).thumbMediaId(), null, null);
//                    case EVENT -> reply = ReplyMessage.ACK; // 地理位置上报事件不允许回复其他消息
                    default -> reply = new ReplyMessage.TextMessage(msg.fromOpenId(), msg.toAccount(),
                            System.currentTimeMillis(), "您好，我不理解您在说什么。不过，我会很快学会的！");
                }
                return CompletableFuture.completedFuture(reply);
            }
        };
    }
}
