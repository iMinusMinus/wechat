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
import weixin.mp.infrastructure.rpc.WeixinUrl;

import java.nio.charset.StandardCharsets;

public class WeixinUserControllerTest extends SpringContainerStarter {

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
    public void testCreateLabelWithIllgalArguments() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "102");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":45158,\"errmsg\":\"tag name too long hint: [iKLbA9sQf-k5ta6] rid: 641db113-442a60c9-6a10f4bc\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> x.getRawPath().startsWith(WeixinUrl.CREATE_LABEL.getPath())), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.post().uri("/mp/label")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"name\":\"瑞信-私人财产神圣不可侵犯\"}")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void testCreateLabel() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "43");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"tag\":{\"id\":100,\"name\":\"福岛核废水\"}}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> x.getRawPath().startsWith(WeixinUrl.CREATE_LABEL.getPath())), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.post().uri("/mp/label")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"福岛核废水\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.id").exists();
    }

    @Test
    public void testListLabel() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "43");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"tags\":[{\"id\":2,\"name\":\"星标组\",\"count\":0},{\"id\":100,\"name\":\"福岛核废水\",\"count\":0}]}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.GET), Mockito.argThat(x -> x.getRawPath().startsWith(WeixinUrl.RETRIEVE_LABEL.getPath())), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get().uri("/mp/label")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$").isArray();
    }

    @Test
    public void testReviseLabel() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "27");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"ok\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> x.getRawPath().startsWith(WeixinUrl.REVISE_LABEL.getPath())), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.put().uri("/mp/label")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"福岛核废水-环保\",\"id\":100}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testRemoveSysLabel() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "106");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":45058,\"errmsg\":\"can't modify sys tag hint: [tKLbJU5Vf-i7JcqA] rid: 641db62d-7a8b279d-14e6f697\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> x.getRawPath().startsWith(WeixinUrl.REMOVE_LABEL.getPath())), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.delete().uri("/mp/label?labelId=2")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void testMark() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "27");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"ok\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> x.getRawPath().startsWith(WeixinUrl.LABELING.getPath())), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.post().uri("/mp/label/100")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("[\"ooUo26seZPcU3qKfcMiXLneG3fO4\"]")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testGetLabelOfUser() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "20");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"tagid_list\":[100]}";

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get().uri("/mp/user/ooUo26seZPcU3qKfcMiXLneG3fO4?labelOnly=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.labels").isArray()
                .jsonPath("$.openId").doesNotExist();
    }

    @Test
    public void testGetUserInfo() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "303");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"subscribe\":1,\"openid\":\"ooUo26seZPcU3qKfcMiXLneG3fO4\",\"nickname\":\"\",\"sex\":0,\"language\":\"zh_CN\",\"city\":\"\",\"province\":\"\",\"country\":\"\",\"headimgurl\":\"\",\"subscribe_time\":1670592443,\"remark\":\"\",\"groupid\":100,\"tagid_list\":[100],\"subscribe_scene\":\"ADD_SCENE_QR_CODE\",\"qr_scene\":110,\"qr_scene_str\":\"scene test\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.GET), Mockito.argThat(x -> true), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get().uri("/mp/user/ooUo26seZPcU3qKfcMiXLneG3fO4")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.labels").isArray()
                .jsonPath("$.openId").exists();
    }

    @Test
    public void testUnmark() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "27");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"ok\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> x.getRawPath().startsWith(WeixinUrl.UNLABELING.getPath())), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.put().uri("/mp/label/100")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("[\"ooUo26seZPcU3qKfcMiXLneG3fO4\"]")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testRemoveLabel() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "27");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"ok\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> true), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.delete().uri("/mp/label?labelId=100")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void testUpdateRemark() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "27");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"ok\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> x.getRawPath().startsWith(WeixinUrl.REMARK_FANS.getPath())), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.put().uri("/mp/user/ooUo26seZPcU3qKfcMiXLneG3fO4?remark=x")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testBatchRetrieveFans() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "324");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"user_info_list\":[{\"subscribe\":1,\"openid\":\"ooUo26seZPcU3qKfcMiXLneG3fO4\",\"nickname\":\"\",\"sex\":0,\"language\":\"zh_CN\",\"city\":\"\",\"province\":\"\",\"country\":\"\",\"headimgurl\":\"\",\"subscribe_time\":1670592443,\"remark\":\"\",\"groupid\":101,\"tagid_list\":[101],\"subscribe_scene\":\"ADD_SCENE_QR_CODE\",\"qr_scene\":110,\"qr_scene_str\":\"scene test\"}]}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> true), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get().uri("/mp/user?user=ooUo26seZPcU3qKfcMiXLneG3fO4&language=zh_CN")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$").isArray();
    }

    @Test
    public void testRetrieveFansIdWithCursor() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "38");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"total\":1,\"count\":0,\"next_openid\":\"\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.GET), Mockito.argThat(x -> true), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get().uri("/mp/user?blocked=false")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isNumber();
    }

    @Test
    public void testRetrieveFansIdWithoutCursor() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "117");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"total\":1,\"count\":1,\"data\":{\"openid\":[\"ooUo26seZPcU3qKfcMiXLneG3fO4\"]},\"next_openid\":\"ooUo26seZPcU3qKfcMiXLneG3fO4\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.GET), Mockito.argThat(x -> true), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get().uri("/mp/user?blocked=false")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items").isArray()
                .jsonPath("$.total").isNumber();
    }

    @Test
    public void testBlockFans() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "27");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"ok\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> x.getRawPath().startsWith(WeixinUrl.BLOCK_FANS.getPath())), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.put().uri("/mp/user?block=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testRetrieveBlockedFans() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "117");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"total\":1,\"count\":1,\"data\":{\"openid\":[\"ooUo26seZPcU3qKfcMiXLneG3fO4\"]},\"next_openid\":\"ooUo26seZPcU3qKfcMiXLneG3fO4\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> true), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get().uri("/mp/user?blocked=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isNumber()
                .jsonPath("$.items").isArray();
    }

    @Test
    public void testUnblockFans() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Length", "27");
        headers.add("Content-Type", "application/json; encoding=utf-8");
        String body = "{\"errcode\":0,\"errmsg\":\"ok\"}";

        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> x.getRawPath().startsWith(WeixinUrl.UNBLOCK_FANS.getPath())), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.put().uri("/mp/user?unblock=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .exchange()
                .expectStatus().isOk();
    }
}
