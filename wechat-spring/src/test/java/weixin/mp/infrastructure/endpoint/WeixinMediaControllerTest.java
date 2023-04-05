package weixin.mp.infrastructure.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
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

public class WeixinMediaControllerTest extends SpringContainerStarter {

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
    public void testCreateDraft() {
        String body = "{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFqsVBT8jSL9j-GK_Gvmyj9lcolbeqT5pCY2s9ExbGWW_\",\"item\":[]}";
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "text/plain");
        headers.add("Content-Length", "89");

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        String copyFromMp = """
                {
                    "articles": [
                        {
                            "title":"图文标题1",
                            "author":"佚名",
                            "content":"<html><head><title>四库全书</title></head><body>永乐大典</body></html>",
                            "contentSourceUrl":"https://imisnuminus.github.io",
                            "thumbMediaId":"BJvvIFQ7gP1o40JR7X1fFkYFeHBF3kzWBvMDS3wVECVj2A88Eu3Y43iWNTHZIakX",
                            "commentState":-1
                        },
                        {
                            "title":"图文标题2",
                            "author":"佚名",
                            "digest":"白马，马，中国古代辩论",
                            "content":"<html><head><title>白马非马</title></head><body>马者，...。白马者，...。</body></html>",
                            "contentSourceUrl":"https://imisnuminus.github.io",
                            "thumbMediaId":"BJvvIFQ7gP1o40JR7X1fFkYFeHBF3kzWBvMDS3wVECVj2A88Eu3Y43iWNTHZIakX",
                            "commentState":1
                        }
                    ]
                }""";
        webClient.post()
                .uri("/mp/paper/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(copyFromMp)
                .exchange()
                .expectBody()
                .jsonPath("$").isEqualTo("BJvvIFQ7gP1o40JR7X1fFqsVBT8jSL9j-GK_Gvmyj9lcolbeqT5pCY2s9ExbGWW_");
    }

    @Test
    public void testGetDraft() {
        String body = "{\"news_item\":[{\"title\":\"图文标题1\",\"author\":\"佚名\",\"digest\":\"永乐大典\",\"content\":\"<html><head><title>四库全书<\\/title><\\/head><body>永乐大典<\\/body><\\/html>\",\"content_source_url\":\"https:\\/\\/imisnuminus.github.io\",\"thumb_media_id\":\"BJvvIFQ7gP1o40JR7X1fFkYFeHBF3kzWBvMDS3wVECVj2A88Eu3Y43iWNTHZIakX\",\"show_cover_pic\":0,\"url\":\"http:\\/\\/mp.weixin.qq.com\\/s?__biz=MzkyOTQyNjU5Mw==&tempkey=MTIxMV9vOGdYQ0VKS29HcVBFRWhMRXlQTEFkTEMyS2llTkRpZ3E5SmlmRWJFMFZSSmduYWdVMzZWMWdpY0psSEZSOHRzNHpQbmYyR2M2M0N3eHpJN2pQLTEyUUh4aDYwdFVKMGpYbjBGQmR4LTFiNkZqS1Z4WVFBcFQtaENwQzFyZnBhc1ZRQk1qYjZqYXFTaUM0QmtKRGdTUFJDRXBqeUY1NndVUTFWalJ3fn4%3D&chksm=4208fced757f75fba5656a85c688da84b5f0316e83f300045466ce12470ba7b2cee808619537#rd\",\"thumb_url\":\"http:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"need_open_comment\":1,\"only_fans_can_comment\":1},{\"title\":\"图文标题2\",\"author\":\"佚名\",\"digest\":\"马者，...。白马者，...。\",\"content\":\"<html><head><title>白马非马<\\/title><\\/head><body>马者，...。白马者，...。<\\/body><\\/html>\",\"content_source_url\":\"https:\\/\\/imisnuminus.github.io\",\"thumb_media_id\":\"BJvvIFQ7gP1o40JR7X1fFkYFeHBF3kzWBvMDS3wVECVj2A88Eu3Y43iWNTHZIakX\",\"show_cover_pic\":0,\"url\":\"http:\\/\\/mp.weixin.qq.com\\/s?__biz=MzkyOTQyNjU5Mw==&tempkey=MTIxMV90d2E3MDJUVXJGZWhmWlNqRXlQTEFkTEMyS2llTkRpZ3E5SmlmRWJFMFZSSmduYWdVMzZWMWdpY0psSG9FUmZHVFVpa0ZkYUwxRkhHVDlnaXcyUWV1OVJhTnJncUNIRzJJOXd2dmRuUzVUZERYTWZIOVdiQm9McWEwekxueUZyalFRUUJzaktKTXRIWHNoeXNraTBJRTVRT2g1WnltV056bVhTcm93fn4%3D&chksm=4208fced757f75fb3c0629c442980b914c95023bf51bf73d0c615e45aac6a467ec1e9303e48f#rd\",\"thumb_url\":\"http:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"need_open_comment\":1,\"only_fans_can_comment\":0}],\"create_time\":1680611910,\"update_time\":1680611910}";
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "text/plain");
        headers.add("Content-Length", "1004");

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get()
                .uri("/mp/paper/draft/BJvvIFQ7gP1o40JR7X1fFqsVBT8jSL9j-GK_Gvmyj9lcolbeqT5pCY2s9ExbGWW_")
                .exchange()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$[0].url").exists();
    }

    @Test // TODO
    public void testReviseDraft() {
        String body = "{\"errcode\":0,\"errmsg\":\"ok\"}";
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "text/plain");
        headers.add("Content-Length", "27");

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.put()
                .uri("/mp/paper/draft/BJvvIFQ7gP1o40JR7X1fFqsVBT8jSL9j-GK_Gvmyj9lcolbeqT5pCY2s9ExbGWW_")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"title\":\"修改图文标题1\",\"author\":\"佚名\",\"digest\":\"永乐大典\",\"content\":\"<html><head><title>四库全书<\\/title><\\/head><body>永乐大典<\\/body><\\/html>\",\"contentSourceUrl\":\"https:\\/\\/imisnuminus.github.io\",\"thumbMediaId\":\"BJvvIFQ7gP1o40JR7X1fFkYFeHBF3kzWBvMDS3wVECVj2A88Eu3Y43iWNTHZIakX\",\"displayCover\":false}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testCountDraft() {
        String body = "{\"total_count\":1}";
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "text/plain");
        headers.add("Content-Length", "17");

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get()
                .uri("/mp/paper/draft")
                .exchange()
                .expectBody()
                .jsonPath("$").isNumber();
    }

    @Test
    public void testListDrafts() {
        String body = "{\"item\":[{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFq1B5poxgLjxEEVTdk20mx81sAaOWJlRCpyQKhUGpBiP\",\"content\":{\"news_item\":[{\"title\":\"修改图文标题1\",\"author\":\"佚名\",\"digest\":\"永乐大典\",\"content\":\"<html><head><title>四库全书<\\/title><\\/head><body>永乐大典<\\/body><\\/html>\",\"content_source_url\":\"https:\\/\\/imisnuminus.github.io\",\"thumb_media_id\":\"BJvvIFQ7gP1o40JR7X1fFkYFeHBF3kzWBvMDS3wVECVj2A88Eu3Y43iWNTHZIakX\",\"show_cover_pic\":0,\"url\":\"http:\\/\\/mp.weixin.qq.com\\/s?__biz=MzkyOTQyNjU5Mw==&tempkey=MTIxMV9MbWl5ZjdMNEpON3NPU005RXlQTEFkTEMyS2llTkRpZ3E5SmlmRWJFMFZSSmduYWdVMzZWMWdpY0psSEZSOHRzNHpQbmYyR2M2M0N3eHpJN2pQLTEyUUh4aDYwdFVKMGpYbjBGQmR4LTFiNkZqS1Z4WVFBcFQtaENwQzFyZnBhc1ZRQk1qYjZqYXFTaUM0QmtXT3RLT1NCYXM4bnpPUE1uODY4cnlnfn4%3D&chksm=4208fced757f75fba5656a85c688da84b5f0316e83f300045466ce12470ba7b2cee808619537#rd\",\"thumb_url\":\"http:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"need_open_comment\":1,\"only_fans_can_comment\":1},{\"title\":\"图文标题2\",\"author\":\"佚名\",\"digest\":\"马者，...。白马者，...。\",\"content\":\"<html><head><title>白马非马<\\/title><\\/head><body>马者，...。白马者，...。<\\/body><\\/html>\",\"content_source_url\":\"https:\\/\\/imisnuminus.github.io\",\"thumb_media_id\":\"BJvvIFQ7gP1o40JR7X1fFkYFeHBF3kzWBvMDS3wVECVj2A88Eu3Y43iWNTHZIakX\",\"show_cover_pic\":0,\"url\":\"http:\\/\\/mp.weixin.qq.com\\/s?__biz=MzkyOTQyNjU5Mw==&tempkey=MTIxMV9ScU54bFh3R1g5dlFMejNURXlQTEFkTEMyS2llTkRpZ3E5SmlmRWJFMFZSSmduYWdVMzZWMWdpY0psSG9FUmZHVFVpa0ZkYUwxRkhHVDlnaXcyUWV1OVJhTnJncUNIRzJJOXd2dmRuUzVUZERYTWZIOVdiQm9McWEwekxueUZyalFRUUJzaktKTXRIWHNoeXN5TW1GZHA1VE1nMmVFT0hFa2hzOVhRfn4%3D&chksm=4208fced757f75fb3c0629c442980b914c95023bf51bf73d0c615e45aac6a467ec1e9303e48f#rd\",\"thumb_url\":\"http:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"need_open_comment\":1,\"only_fans_can_comment\":0}],\"create_time\":1680611910,\"update_time\":1680611910},\"update_time\":1680611910}],\"total_count\":1,\"item_count\":1}";
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "text/plain");
        headers.add("Content-Length", "1073");

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get()
                .uri("/mp/paper/draft?offset=0&count=10")
                .exchange()
                .expectBody()
                .jsonPath("$.total").isNumber()
                .jsonPath("$.items").isArray()
                .jsonPath("$.items[0].id").exists()
                .jsonPath("$.items[0].updatedAt").exists()
                .jsonPath("$.items[0].items").isArray();
    }

    @Test
    public void testDeleteDraft() {
        String body = "{\"errcode\":0,\"errmsg\":\"ok\"}";
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "text/plain");
        headers.add("Content-Length", "27");

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.delete()
                .uri("/mp/paper/draft/BJvvIFQ7gP1o40JR7X1fFq1B5poxgLjxEEVTdk20mx81sAaOWJlRCpyQKhUGpBiP")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testPublish() {
        String body = "{\"errcode\":0,\"errmsg\":\"ok\",\"publish_id\":2247483663,\"msg_data_id\":2247483663}";
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "text/plain");
        headers.add("Content-Length", "76");

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.post()
                .uri("/mp/paper?mediaId=BJvvIFQ7gP1o40JR7X1fFqsVBT8jSL9j-GK_Gvmyj9lcolbeqT5pCY2s9ExbGWW_")
                .exchange()
                .expectBody()
                .jsonPath("$.publishId").exists();
    }

    @Test
    public void testGetPublishStatus() {
        String body = "{\"publish_id\":2247483663,\"publish_status\":0,\"article_id\":\"xmLI4iSa_yfkVNgF-U-S5HL1SLrB-B_rCYYKTKbLJAqE7pLGiSdHRUPJ-nf8ccYJ\",\"article_detail\":{\"count\":2,\"item\":[{\"idx\":1,\"article_url\":\"http:\\/\\/mp.weixin.qq.com\\/s?__biz=MzkyOTQyNjU5Mw==&mid=2247483663&idx=1&sn=fa59d2eaafbe5efb7d8232d8785cf9d7&chksm=c208fcc1f57f75d70b634306de41c07f4124d961ce99e47914b7426a63aa61f7b16357a70750#rd\"},{\"idx\":2,\"article_url\":\"http:\\/\\/mp.weixin.qq.com\\/s?__biz=MzkyOTQyNjU5Mw==&mid=2247483663&idx=2&sn=c7ed40087126664e0ea401afe4dedf77&chksm=c208fcc1f57f75d78e4878c1f97550ac3cf329a1c499d71056b325fed3a3ccd106c745bd983f#rd\"}]},\"fail_idx\":[]}";
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "text/plain");
        headers.add("Content-Length", "27");

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get()
                .uri("/mp/paper?publishId=2247483663")
                .exchange()
                .expectBody()
                .jsonPath("$.status").exists()
                .jsonPath("$.articleId").exists()
                .jsonPath("$.successArticles").isArray();
    }

    @Test
    public void testCancelPublish() {
        String body = "{\"errcode\":0,\"errmsg\":\"ok\"}";
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "text/plain");
        headers.add("Content-Length", "27");

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.delete()
                .uri("/mp/paper/xmLI4iSa_yfkVNgF-U-S5HL1SLrB-B_rCYYKTKbLJAqE7pLGiSdHRUPJ-nf8ccYJ")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testRetrievePublishedArticle() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "text/plain");
        headers.add("Content-Length", "759");
        String body = "{\"news_item\":[{\"title\":\"图文标题1\",\"author\":\"佚名\",\"digest\":\"永乐大典\",\"content\":\"<html><head><title>四库全书<\\/title><\\/head><body>永乐大典<\\/body><\\/html>\",\"content_source_url\":\"https:\\/\\/imisnuminus.github.io\",\"thumb_media_id\":\"BJvvIFQ7gP1o40JR7X1fFkYFeHBF3kzWBvMDS3wVECVj2A88Eu3Y43iWNTHZIakX\",\"show_cover_pic\":0,\"url\":\"http:\\/\\/mp.weixin.qq.com\\/s?__biz=MzkyOTQyNjU5Mw==&mid=2247483663&idx=1&sn=fa59d2eaafbe5efb7d8232d8785cf9d7&chksm=c208fcc1f57f75d70b634306de41c07f4124d961ce99e47914b7426a63aa61f7b16357a70750#rd\",\"thumb_url\":\"http:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"need_open_comment\":0,\"only_fans_can_comment\":1,\"is_deleted\":false},{\"title\":\"图文标题2\",\"author\":\"佚名\",\"digest\":\"白马，马，中国古代辩论\",\"content\":\"<html><head><title>白马非马<\\/title><\\/head><body>马者，...。白马者，...。<\\/body><\\/html>\",\"content_source_url\":\"https:\\/\\/imisnuminus.github.io\",\"thumb_media_id\":\"BJvvIFQ7gP1o40JR7X1fFkYFeHBF3kzWBvMDS3wVECVj2A88Eu3Y43iWNTHZIakX\",\"show_cover_pic\":0,\"url\":\"http:\\/\\/mp.weixin.qq.com\\/s?__biz=MzkyOTQyNjU5Mw==&mid=2247483663&idx=2&sn=c7ed40087126664e0ea401afe4dedf77&chksm=c208fcc1f57f75d78e4878c1f97550ac3cf329a1c499d71056b325fed3a3ccd106c745bd983f#rd\",\"thumb_url\":\"http:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"need_open_comment\":1,\"only_fans_can_comment\":0,\"is_deleted\":false}],\"create_time\":1678524405,\"update_time\":1678524432}";

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get()
                .uri("/mp/paper/xmLI4iSa_yfkVNgF-U-S5HL1SLrB-B_rCYYKTKbLJAqE7pLGiSdHRUPJ-nf8ccYJ")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testPublishedArticles() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "text/plain");
        headers.add("Content-Length", "1066");
        String body = "{\"item\":[{\"article_id\":\"xmLI4iSa_yfkVNgF-U-S5HL1SLrB-B_rCYYKTKbLJAqE7pLGiSdHRUPJ-nf8ccYJ\",\"content\":{\"news_item\":[{\"title\":\"图文标题1\",\"author\":\"佚名\",\"digest\":\"永乐大典\",\"content_source_url\":\"https:\\/\\/imisnuminus.github.io\",\"thumb_media_id\":\"BJvvIFQ7gP1o40JR7X1fFkYFeHBF3kzWBvMDS3wVECVj2A88Eu3Y43iWNTHZIakX\",\"show_cover_pic\":0,\"url\":\"http:\\/\\/mp.weixin.qq.com\\/s?__biz=MzkyOTQyNjU5Mw==&mid=2247483663&idx=1&sn=fa59d2eaafbe5efb7d8232d8785cf9d7&chksm=c208fcc1f57f75d70b634306de41c07f4124d961ce99e47914b7426a63aa61f7b16357a70750#rd\",\"thumb_url\":\"http:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"need_open_comment\":0,\"only_fans_can_comment\":1,\"is_deleted\":false},{\"title\":\"图文标题2\",\"author\":\"佚名\",\"digest\":\"白马，马，中国古代辩论\",\"content_source_url\":\"https:\\/\\/imisnuminus.github.io\",\"thumb_media_id\":\"BJvvIFQ7gP1o40JR7X1fFkYFeHBF3kzWBvMDS3wVECVj2A88Eu3Y43iWNTHZIakX\",\"show_cover_pic\":0,\"url\":\"http:\\/\\/mp.weixin.qq.com\\/s?__biz=MzkyOTQyNjU5Mw==&mid=2247483663&idx=2&sn=c7ed40087126664e0ea401afe4dedf77&chksm=c208fcc1f57f75d78e4878c1f97550ac3cf329a1c499d71056b325fed3a3ccd106c745bd983f#rd\",\"thumb_url\":\"http:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"need_open_comment\":1,\"only_fans_can_comment\":0,\"is_deleted\":false}],\"create_time\":1678524405,\"update_time\":1678524432},\"update_time\":1678524432}],\"total_count\":1,\"item_count\":1}";

        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get()
                .uri("/mp/paper?offset=0&count=10")
                .exchange()
                .expectBody()
                .jsonPath("$.total").isNumber()
                .jsonPath("$.items").isArray()
                .jsonPath("$.items[0].items").isArray()
                .jsonPath("$.items[0].items[0].deleted").exists();
    }

    @Test
    public void testEnableComment() {
        // TODO
    }

    @Test
    public void testDisableComment() {
        // TODO
    }

    @Test
    public void testViewComment() {
        // TODO
    }

    @Test
    public void testMoveCommentToTop() {
        // TODO
    }

    @Test
    public void testCancelTopComment() {
        // TODO
    }

    @Test
    public void testDeleteComment() {
        // TODO
    }

    @Test
    public void testRelyComment() {
        // TODO
    }

    @Test
    public void testDeleteReply() {
        // TODO
    }
}
