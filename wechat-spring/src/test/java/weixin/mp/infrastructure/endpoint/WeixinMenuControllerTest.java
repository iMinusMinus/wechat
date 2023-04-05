package weixin.mp.infrastructure.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.context.ActiveProfiles;
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

@ActiveProfiles("junit")
public class WeixinMenuControllerTest extends SpringContainerStarter {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private CacheManager cacheManager;

    @Mock
    private ClientHttpConnector clientHttpConnector;

    @Mock
    private ClientHttpResponse clientHttpResponse;

    private static final MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();

    @BeforeEach
    public void setUp() {
        mvm.add(HttpHeaders.CONTENT_TYPE, "application/json; encoding=utf-8");

        WeixinTest.customizerWeixinJavaClient(WebClient.builder().clientConnector(clientHttpConnector));
        cacheManager.getCache(CacheName.ACCESS_TOKEN).put(CacheKey.TOKEN_FMT.formatted("wxd3a0f6c8176edcab"), new Weixin.AuthenticationResponse(0, null, "66_QPewa1GdoE8rvPukCmRJPpXRxQdn85FPX8Ku9KNNHjB3JdbH8MGAe0mwWC6OnMn551MYxPVUmTKQcuCVfitPwo56rTfuDtAMwH7UvCJ6gpP8Tew7coVxq5utLUYFGCbAEAABX", 7200));

    }

    @Test
    @DisplayName("小程序按钮缺少参数url")
    public void testCreateMenuWithIllegalParam() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(mvm);
        headers.add("Content-Length", "88");
        String body = """
                {
                    "errcode": 40027,
                    "errmsg": "invalid sub button url size rid: 640d8707-66796d60-192e4bee"
                }""";
        Mockito.when(clientHttpConnector.connect(Mockito.eq(HttpMethod.POST), Mockito.argThat(x -> x.getRawPath().startsWith(WeixinUrl.CREATE_MENU.getPath())), Mockito.argThat(x -> true)))
                .thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        String request = """
                {
                    "button":[
                        {"name":"Peoples","sub_button":[
                            {"name":"时事","type":"view","url":"https://timesmagazin.com/category/world"},
                            {"name":"人和","type":"miniprogram","appId":"gh_d8e29764970b", "pagePath":"pages/welcome/welcome"},
                            {"name":"见天地","type":"pic_photo_or_album","key":"hengqu1"},
                            {"name":"见自己","type":"pic_sysphoto","key":"hengqu3"},
                            {"name":"见众生","type":"pic_weixin","key":"hengqu2"}
                        ]},
                        {"name":"Things","sub_button":[
                            {"name":"Practice","type":"click","key":"PRACTICE_MAKES_SENSE"},
                            {"name":"Survey","type":"scancode_push","key":"WITHOUT_AN_INVESTIGATION_THERE_IS_NO_VOICE"},
                            {"name":"Listen","type":"scancode_waitmsg","key":"LISTENING_IS_BRIGHT_FAITH_IS_DARK"},
                            {"name":"See","type":"pic_photo_or_album","key":"LEARN_FROM_HISTORY"},
                            {"name":"Think","type":"article_id","key":"I_THINK_THEREFORE_I_AM","article_id":"xmLI4iSa_yfkVNgF-U-S5HL1SLrB-B_rCYYKTKbLJAqE7pLGiSdHRUPJ-nf8ccYJ"}
                        ]},
                        {"name":"Knowledge","sub_button":[
                            {"name":"Struggle","type":"media_id","key":"LIVE_FREE_OR_DIE","media_id":"BJvvIFQ7gP1o40JR7X1fFkfMtFIwPH-lDun_28LVZtv4Hlik3wFjSu6jVH4lzJYB"},
                            {"name":"Position","type":"location_select","key":"FITTEST_SURVIVAL_LAST"},
                            {"name":"Trial","type":"article_view_limited","key":"IMAGINE_FANTASY_CAREFUL_VERIFICATION","article_id":"xmLI4iSa_yfkVNgF-U-S5HL1SLrB-B_rCYYKTKbLJAqE7pLGiSdHRUPJ-nf8ccYJ"}
                        ]}
                    ]
                }""";
        webClient.post()
                .uri(("/mp/menu"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void testCreateMenu() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(mvm);
        headers.add("Content-Length", "27");
        String body = """
                {
                    "errcode": 0,
                    "errmsg": "ok"
                }""";
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        String request = """
                {
                    "button":[
                        {"name":"Peoples","sub_button":[
                            {"name":"时事","type":"view","url":"https://timesmagazin.com/category/world"},
                            {"name":"见天地","type":"pic_photo_or_album","key":"hengqu1"},
                            {"name":"见自己","type":"pic_sysphoto","key":"hengqu3"},
                            {"name":"见众生","type":"pic_weixin","key":"hengqu2"}
                        ]},
                        {"name":"Things","sub_button":[
                            {"name":"Practice","type":"click","key":"PRACTICE_MAKES_SENSE"},
                            {"name":"Survey","type":"scancode_push","key":"WITHOUT_AN_INVESTIGATION_THERE_IS_NO_VOICE"},
                            {"name":"Listen","type":"scancode_waitmsg","key":"LISTENING_IS_BRIGHT_FAITH_IS_DARK"},
                            {"name":"See","type":"pic_photo_or_album","key":"LEARN_FROM_HISTORY"},
                            {"name":"Think","type":"article_id","key":"I_THINK_THEREFORE_I_AM","article_id":"xmLI4iSa_yfkVNgF-U-S5HL1SLrB-B_rCYYKTKbLJAqE7pLGiSdHRUPJ-nf8ccYJ"}
                        ]},
                        {"name":"Knowledge","sub_button":[
                            {"name":"Struggle","type":"media_id","key":"LIVE_FREE_OR_DIE","media_id":"BJvvIFQ7gP1o40JR7X1fFkfMtFIwPH-lDun_28LVZtv4Hlik3wFjSu6jVH4lzJYB"},
                            {"name":"Position","type":"location_select","key":"FITTEST_SURVIVAL_LAST"},
                            {"name":"Trial","type":"article_view_limited","key":"IMAGINE_FANTASY_CAREFUL_VERIFICATION","article_id":"xmLI4iSa_yfkVNgF-U-S5HL1SLrB-B_rCYYKTKbLJAqE7pLGiSdHRUPJ-nf8ccYJ"}
                        ]}
                    ]
                }""";
        webClient.post()
                .uri(("/mp/menu"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testCreateCustomMenu() {
        // TODO
    }

    @Test
    public void testGetMenu() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(mvm);
        String body = "{\"is_menu_open\":1,\"selfmenu_info\":{\"button\":[{\"name\":\"发送消息\",\"sub_button\":{\"list\":[{\"type\":\"img\",\"name\":\"图片\",\"value\":\"6kITFwk9KA4ZUngkwNPerfV-ZREgFqFAe5PVsi4hH3nEotIgT_bLOD6p3O5ufO8x\"},{\"type\":\"voice\",\"name\":\"音频\",\"value\":\"6kITFwk9KA4ZUngkwNPerR7hZx6u10Xy7iL_rRlcIg75CN9hhMjSzeeYnuoBnIGy\"},{\"type\":\"video\",\"name\":\"视频\",\"value\":\"http:\\/\\/mp.weixin.qq.com\\/mp\\/mp\\/video?__biz=MzI0MzI4OTYzOA==&mid=502889239&sn=39e0f854ac5c626f836194fcf70009cc&vid=wxv_2830117429317156864&idx=1&vidsn=5513d6487e4915f0eb44392f4ba263fc&fromid=1#rd\"},{\"type\":\"news\",\"name\":\"图文消息\",\"value\":\"AznoOhxCzNi9uKm528SyhEHWxKT9XB6Juv3QymHBmgwWnS0elzYsxzfXX7kA-jDe\",\"news_info\":{\"list\":[{\"title\":\"Hello \u200BWorld ！\",\"author\":\"\",\"digest\":\"分享一篇文章。\",\"show_cover\":0,\"cover_url\":\"https:\\/\\/mmbiz.qpic.cn\\/sz_mmbiz_jpg\\/QaUEicC4Us5MSkm4mfibywg3nWruAIwSZG2r6pna3ytQicHUKUAxw3m8icdsnaOKsSkLuh8Of8RwPquxpSgotg5pdw\\/0?wx_fmt=jpeg\",\"content_url\":\"http:\\/\\/mp.weixin.qq.com\\/s?__biz=MzI0MzI4OTYzOA==&mid=2650372882&idx=1&sn=5cce74fde652bc7731f612e92a20389c&chksm=f162805bc615094d20718b5bef5d954c5aa38e92fdc0a67425d009467f28ed44ac3910a635ea#rd\",\"source_url\":\"\"}]}}]}},{\"type\":\"view\",\"name\":\"跳转网页\",\"url\":\"http:\\/\\/mp.weixin.qq.com\\/s?__biz=MzI0MzI4OTYzOA==&mid=2650372889&idx=1&sn=ebc27e21b710dc928f3be9bb70811112&chksm=f1628050c61509463f629dd9d03a492df6e7b3644041eb80eeadb6b7677bfa16d9f34a279da6&scene=18#wechat_redirect\"},{\"type\":\"miniprogram\",\"name\":\"跳小程序\",\"url\":\"\",\"appid\":\"wxf90a590231065432\",\"pagepath\":\"pages\\/welcome\\/welcome\"}]}}";
        // "{\"is_menu_open\":1,\"selfmenu_info\":{\"buttons\":[{\"name\":\"Peoples\",\"buttons\":{\"list\":[{\"type\":\"view\",\"name\":\"时事\",\"url\":\"https://timesmagazin.com/category/world\"},{\"type\":\"pic_photo_or_album\",\"name\":\"见天地\",\"key\":\"hengqu1\"},{\"type\":\"pic_sysphoto\",\"name\":\"见自己\",\"key\":\"hengqu3\"},{\"type\":\"pic_weixin\",\"name\":\"见众生\",\"key\":\"hengqu2\"}]}},{\"name\":\"Things\",\"buttons\":{\"list\":[{\"type\":\"click\",\"name\":\"Practice\",\"key\":\"PRACTICE_MAKES_SENSE\"},{\"type\":\"scancode_push\",\"name\":\"Survey\",\"key\":\"WITHOUT_AN_INVESTIGATION_THERE_IS_NO_VOICE\"},{\"type\":\"scancode_waitmsg\",\"name\":\"Listen\",\"key\":\"LISTENING_IS_BRIGHT_FAITH_IS_DARK\"},{\"type\":\"pic_photo_or_album\",\"name\":\"See\",\"key\":\"LEARN_FROM_HISTORY\"},{\"type\":\"article_id\",\"name\":\"Think\",\"articleId\":\"xmLI4iSa_yfkVNgF-U-S5HL1SLrB-B_rCYYKTKbLJAqE7pLGiSdHRUPJ-nf8ccYJ\"}]}},{\"name\":\"Knowledge\",\"buttons\":{\"list\":[{\"type\":\"media_id\",\"name\":\"Struggle\",\"mediaId\":\"BJvvIFQ7gP1o40JR7X1fFkfMtFIwPH-lDun_28LVZtv4Hlik3wFjSu6jVH4lzJYB\"},{\"type\":\"location_select\",\"name\":\"Position\",\"key\":\"FITTEST_SURVIVAL_LAST\"},{\"type\":\"article_view_limited\",\"name\":\"Trial\",\"articleId\":\"xmLI4iSa_yfkVNgF-U-S5HL1SLrB-B_rCYYKTKbLJAqE7pLGiSdHRUPJ-nf8ccYJ\"}]}}]},\"enabled\":true}";
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.get()
                .uri("/mp/menu")
                .exchange()
                .expectBody()
                .jsonPath("$.fixedMenu").exists()
                .jsonPath("$.conditionalMenus").doesNotExist()
                .jsonPath("$.fixedMenu.items[0].name").isEqualTo("发送消息")
                .jsonPath("$.fixedMenu.items[2].name").isEqualTo("跳小程序");
    }

    @Test
    public void testMatchCustomMenu() {
        // TODO
    }

    @Test
    public void testDeleteCustomMenu() {
        // TODO
    }

    @Test
    public void testDeleteMenu() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(mvm);
        headers.add("Content-Length", "27");
        String body = "{\"errcode\":0,\"errmsg\":\"ok\"}";
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))));

        webClient.delete()
                .uri("/mp/menu")
                .exchange()
                .expectStatus().isOk();
    }
}
