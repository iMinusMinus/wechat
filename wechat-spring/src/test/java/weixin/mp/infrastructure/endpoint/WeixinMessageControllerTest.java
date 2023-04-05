package weixin.mp.infrastructure.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import weixin.SpringContainerStarter;
import weixin.mp.infrastructure.cache.CacheKey;
import weixin.mp.infrastructure.cache.CacheName;
import weixin.mp.infrastructure.rpc.Weixin;
import weixin.mp.infrastructure.rpc.WeixinTest;

import java.nio.charset.StandardCharsets;

public class WeixinMessageControllerTest extends SpringContainerStarter {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private CacheManager cacheManager;

    @Mock
    private ClientHttpConnector clientHttpConnector;

    @Mock
    private ClientHttpResponse clientHttpResponse;

    @BeforeEach
    public void setUp() {
        WeixinTest.customizerWeixinJavaClient(WebClient.builder().clientConnector(clientHttpConnector));
        cacheManager.getCache(CacheName.ACCESS_TOKEN).put(CacheKey.TOKEN_FMT.formatted("wxd3a0f6c8176edcab"), new Weixin.AuthenticationResponse(0, null, "66_QPewa1GdoE8rvPukCmRJPpXRxQdn85FPX8Ku9KNNHjB3JdbH8MGAe0mwWC6OnMn551MYxPVUmTKQcuCVfitPwo56rTfuDtAMwH7UvCJ6gpP8Tew7coVxq5utLUYFGCbAEAABX", 7200));

    }

    @Test
    public void testListMsgTpl() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "20");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"template_list\":[]}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.GET), Mockito.argThat(x -> true), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get().uri("/mp/message")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$").isArray()
//                .jsonPath("$[0].content").value(x -> x.contains(".DATA"), String.class)
                ;
    }

    @Test
    public void testSendTplMsg() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "55");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"ok\",\"msgid\":2860528972743540739}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> true), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.post().uri("/mp/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"receiver\":\"ooUo26seZPcU3qKfcMiXLneG3fO4\",\"templateId\":\"jPhQR1ueRFWAhf4FYEZ0-bwFs6oQFXql_gc4v1XcjGA\",\"data\":{\"number01\":{\"value\":\"6a6y\",\"color\":\"#CAFAFE\"}}}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$").isNumber();
    }

    @Test
    public void testDeleteTpl() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "27");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"ok\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.delete().uri("/mp/message?templateId=jPhQR1ueRFWAhf4FYEZ0-bwFs6oQFXql_gc4v1XcjGA")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testBroadcastNews() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "97");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"send job submission success\",\"msg_id\":1000000002,\"msg_data_id\":2247483677}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> true), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.post().uri("/mp/broadcast")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"groupId\":\"100\",\"messageType\":\"mpnews\",\"material\":{\"id\":\"D3--TpU4kMz_JBPSe8LNW97tQq_gAbOgHrXVwAvv2IgcVJrrEkA-OmNh0BUH-sub\"},\"forcePublish\":true}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.msgId").exists()
                .jsonPath("$.msgDataId").exists();
    }

    @Test
    public void testBroadcastText() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "97");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"send job submission success\",\"msg_id\":1000000001,\"msg_data_id\":2247483675}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> true), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.post().uri("/mp/broadcast")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"groupId\":100,\"messageType\":\"text\",\"material\":{\"id\":\"测试按标签群发文本\"}}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testBroadcastImages() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "97");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"send job submission success\",\"msg_id\":1000000003,\"msg_data_id\":2247483680}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> true), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.post().uri("/mp/broadcast")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"messageType\":\"image\",\"material\":{\"imageIds\":[\"BJvvIFQ7gP1o40JR7X1fFkqQFxsyfB88G8PYFymWWnH-08rHQ-Uu6yyyAwDsfPsL\",\"D3--TpU4kMz_JBPSe8LNW5cULH9cHsLETHmXMm5LAZk5VJnFc213OiY7b1xDIEVm\"],\"recommend\":\"群发图片推荐原因\"},\"forcePublish\":true}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.msgId").exists();
    }

    @Test
    public void testBroadcastVoice() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "97");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"send job submission success\",\"msg_id\":3147483652,\"msg_data_id\":2247483682}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> true), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.post().uri("/mp/broadcast")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"users\":[\"ooUo26seZPcU3qKfcMiXLneG3fO4\",\"ooUo26iamMFBHBP4Evjs_9hprn0s\"],\"messageType\":\"voice\",\"material\":{\"id\":\"BJvvIFQ7gP1o40JR7X1fFoACeRk5UI6oMH9bOA40CLkUFwJlvM_NfPaH0CdPy9y4\"}}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.msgId").exists();
    }

    @Test
    public void testBroadcastVideo() {
        // TODO
    }

    @Test
    public void testBroadcastCard() {
        // TODO
    }

    @Test
    public void testBroadcastMusic() {
        // TODO
    }
}
