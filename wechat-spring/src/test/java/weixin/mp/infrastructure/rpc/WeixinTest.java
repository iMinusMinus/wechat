package weixin.mp.infrastructure.rpc;

import org.springframework.web.reactive.function.client.WebClient;

public abstract class WeixinTest {

    public static void customizerWeixinJavaClient(WebClient.Builder builder) {
        Weixin.customizerWebClient(builder);
    }
}
