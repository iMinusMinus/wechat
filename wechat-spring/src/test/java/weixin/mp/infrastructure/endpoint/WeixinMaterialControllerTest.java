package weixin.mp.infrastructure.endpoint;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import weixin.SpringContainerStarter;
import weixin.mp.domain.FileType;
import weixin.mp.infrastructure.cache.CacheKey;
import weixin.mp.infrastructure.cache.CacheName;
import weixin.mp.infrastructure.rpc.InputStreamUtil;
import weixin.mp.infrastructure.rpc.Weixin;
import weixin.mp.infrastructure.rpc.WeixinTest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class WeixinMaterialControllerTest extends SpringContainerStarter {

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
    public void testUploadPermanentPicture() {
        String textBody = "{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFm4RVya9eAEvt1KhscbHlCSYI4btk7TuE85nc4dwNfvw\",\"url\":\"http:\\/\\/mmbiz.qpic.cn\\/mmbiz_png\\/MDXeetibImLXOyjccFrzv24cfrR4wlFDyTEVcDnSNMusdkDLiacSXDiatcl6W4jPXBiav8B8vy8I6eT3tiaPicRJzPcA\\/0?wx_fmt=png\",\"item\":[]}";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "239");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        String base64 = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAACXBIWXMAAAFjAAABYwGNYDK3AAAAGXRFWHRTb2Z0d2FyZQB3d3cuaW5rc2NhcGUub3Jnm+48GgAAABd0RVh0VGl0bGUAVGhlIENlbnRPUyBTeW1ib2yF4TxCAAACjUlEQVQ4jZ1TSUxTURQ9/7dApRMd4JfSIoUyxA5GBUIcYkQiWxKLunLnhh0sjazYwwabGN0ZDWGBIGCMQUJi1BhAMKUMbV9bBEpbOvg76OdTi6s2NGgX3tXNveeee17euUCR4P1To7zvlb0YpsjwtJ2dvnHEvr6e4bdnn/4LR+USf3x+kOWIBQCqzpiaudiiWhTfKaMicurn73OcuPZiJLQV3gKAihqFo7ajaQgAhDkCliOWxcBIb6OqBxuRcaT4PVwR33UJfHX05+c+o6QqqmXMeiuZd+LC/Wt5BfRJOY2qHrDcNnSyy6ekaqy1SAVZNHSaCup5ArXI2hRMLUFaWo04R9Ci7s2DjF1mJPfiEFfJEHTsQGHUtBQQeD9sPg56SGXycBck/gaSEg1+cF4ole0SeX2zJLEbg5iRw7uwjvRBAqtfiGpmZvUZAFBkwWlfffnpQeXV8qTAkIzlmKu1JrFUIZ3LhBXH4f10J/n2PZHruQ9S6rcfPYqB/u4nQoqiswAQDcgy0f0KPq/7klKUTotwlolCxrECRu8qzbUCv0RCAMhmj2kKANzv10ZWXKE7j4bfaQHgtq0Vfl8EfQPWfZMuli1xvqih5UbwZAwAQKQPA37OOmWztfbRANB409xfY9KF9HplflinV2LTvZlOx5fTtFiDLOtBacM90OVatJxvDttsrX0Fv7C2EfC0tRngJQfQ6ZWYmvyafw2/PQNarEE2QSBkOkAnHO6/+mBiYhkGgxpLi75TPshEVkBL9OB9EwX1vJXn5tYHCQlZAMBsYYw7UQcjVLhEderkcTXFHKrk7eFs1OkGAIG83lGmuzV0asvJcEcnh8cd3Udjjq6MK/KfF+mOTNtJbHa0GOYP42QK/fYhovYAAAAASUVORK5CYII=";
        byte[] img = Base64.getDecoder().decode(base64);
        builder.part("file", new ByteArrayResource(img), MediaType.IMAGE_PNG).filename("centos.png");
        webClient.post().uri("mp/asset?permanent=true")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.url").exists();
    }

    @Test
    public void testUploadPermanentThumb() {
        String textBody = "{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFjx046v_4bUWEtPDETawG89eht_7y4WqErBcq54YS8RQ\",\"url\":\"http:\\/\\/mmbiz.qpic.cn\\/mmbiz_jpg\\/MDXeetibImLVy7z6TrEbqcwara9pFamLAVRXek0BBqPeh8SNZGoFuSwRA94TjZSDzibic81oxGGH5ibvoePH0BSo2w\\/0?wx_fmt=jpeg\",\"item\":[]}";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "238");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ClassPathResource("assets/resin_logo_small.jpg"), MediaType.IMAGE_JPEG).filename("resin_logo.jpg");
        webClient.post().uri("mp/asset?permanent=true")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.url").exists();
    }

    @Test
    public void testUploadPermanentAudio() {
        String textBody = "{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFrxu0HqLSRZf_r5T87rPPVzzR-pyBWDnQeLvx-keMwCm\",\"item\":[]}";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "89");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        // javax.swing.plaf.metal.sounds is not open to module wechat.spring
//        InputStream is = MetalTheme.class.getModule()
//                .getResourceAsStream("/javax/swing/plaf/metal/sounds/MenuItemCommand.wav");

        builder.part("file", new ClassPathResource("openai/1949FoundingCeremony.mp3"), MediaType.APPLICATION_OCTET_STREAM).filename("1949FoundingCeremony.mp3");
        webClient.post().uri("mp/asset?permanent=true")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.url").doesNotExist();
    }

    @Test
    public void testUploadPermanentVideo() {
        // {"media_id":"BJvvIFQ7gP1o40JR7X1fFl1KmdrbvveGwXe7cs1nM-niWtR8CHOJEHKFpV45sqMK","item":[]}
        String textBody = "{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFkfMtFIwPH-lDun_28LVZtv4Hlik3wFjSu6jVH4lzJYB\",\"item\":[]}";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "89");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        // http://hprc.cssn.cn/gswsp/syspp/
        // 1964第一颗原子弹爆炸成功.wmv --> http://103.247.176.18/masvod/public/2018/07/26/20180726_164d6066636_r34.mp4
        builder.part("file", new ClassPathResource("assets/20180726_164d6066636_r34.mp4"), MediaType.APPLICATION_OCTET_STREAM).filename("1964第一颗原子弹爆炸成功.mp4");
        WeixinMaterialController.Description description = new WeixinMaterialController.Description("think", "what do you think of ?");
        builder.part("description", description, MediaType.APPLICATION_JSON);
        webClient.post().uri("mp/asset?permanent=true")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.url").doesNotExist();
    }

    @Test
    @DisplayName("测试用于群发的上传图片")
    public void testUploadImage() {
        String textBody = "{\"url\":\"http:\\/\\/mmbiz.qpic.cn\\/mmbiz_jpg\\/MDXeetibImLXq5IB1BsYLvq6KnGxRAhhiaibZ5AbKsV2wwwKRsicTIVI78RzSnE8p2oiaXwRN401LemnzJ3iawicN6rvw\\/0\"}";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "141");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ClassPathResource("assets/resin_logo_small.jpg"), MediaType.APPLICATION_OCTET_STREAM).filename("resin_logo_small.jpg");
        webClient.post().uri("mp/asset?broadcast=true&permanent=true")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectBody()
                .jsonPath("$.url").exists()
                .jsonPath("$.id").doesNotExist()
                .jsonPath("$.createdAt").doesNotExist();
    }

    @Test
    @DisplayName("测试用于群发的上传图文，与评论的新增永久素材、新建草稿区别是什么")
    public void testUploadNews() {
        String textBody = "{\"type\":\"news\",\"media_id\":\"D3--TpU4kMz_JBPSe8LNW97tQq_gAbOgHrXVwAvv2IgcVJrrEkA-OmNh0BUH-sub\",\"created_at\":1680317191,\"item\":[]}";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "127");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        webClient.post().uri("mp/asset?broadcast=true")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"articles\":[{\"thumbMediaId\":\"BJvvIFQ7gP1o40JR7X1fFjx046v_4bUWEtPDETawG89eht_7y4WqErBcq54YS8RQ\",\"title\":\"测试群发用图文消息的标题\",\"content\":\"<p>Hello World!</p>\"}]}")
                .exchange()
                .expectBody()
                .jsonPath("$.type").exists()
                .jsonPath("$.id").exists()
                .jsonPath("$.createdAt").exists();
    }

    @Test
    @DisplayName("下载临时素材")
    public void testDownloadTempMaterial() {
        String printableText = """
                ÿØÿàJFIFÿþ1Image Resized at http://www.shrinkpictures.com
                ÿÛC
                
                
                
                                
                ÿÛC		
                
                ÿÀ#U"ÿÄ	
                ÿÄµ}!1AQa"q2¡#B±ÁRÑð$3br	
                %&'()*456789:CDEFGHIJSTUVWXYZcdefghijstuvwxyz¢£¤¥¦§¨©ª²³´µ¶·¸¹ºÂÃÄÅÆÇÈÉÊÒÓÔÕÖ×ØÙÚáâãäåæçèéêñòóôõö÷øùúÿÄ	
                ÿÄµw!1AQaq"2B¡±Á	#3RðbrÑ
                $4á%ñ&'()*56789:CDEFGHIJSTUVWXYZcdefghijstuvwxyz¢£¤¥¦§¨©ª²³´µ¶·¸¹ºÂÃÄÅÆÇÈÉÊÒÓÔÕÖ×ØÙÚâãäåæçèéêòóôõö÷øùúÿÚ?ýR¤Î(Í¤J47µ³LRÖ}góïìÝû['íãxu|!áïøGeØ·7Ro¯ÉGÿ.väñx#NS¦s9Td¢Þ¬åþ=üg]ûÂ³[jþ¿´º[ÞÚÈ±ÎÃäg
                z?ìéñq¼wáÓ¥êÒâ9BË¸ó<Ã õô?zë~0xrM{áæ¾¶%»®Ge3iÖ×Ñ«+\\>XÉÆ9Çqõù]¢üøûñ3âf}xT¡au%¯Øª¦âÊ[in8;c_EAa±xWKàk«}É-YWÃ×Søòþ¶?aª©$s^yâ~Ó/$Ó4µø»\\C´iÜ¸oI~î/«²×Äw7?ôùEøã=FÿP(lµ%òlå^r§Ë(>¯kë/Ùo6yo®iv4iÓä°(ãçhå+s\\u²×¥í§$×eúw:icUjÎ)¦zµ_ëV3\\øA´ðôÿ¸³Kï´Ê©ÿMT(oe,=è®¯¢¼fîîzYØÁP5ßCmá?Z®¹«ÙÈZhlî^ê8à^²1:cßµ{7ÆÛNÇàÃ_êúÿn<KjÚøJ	ÃK dHûr,ÝÉ9pkÆ¿àvp¯~']y`\\6FLs´#?2ký¿4kãûfü=¸¼ñÓï,íã²ñÍËbë+ã®ÊIÏ³_DðøybÞBÊ7{½t<8Ö¬°ê³ÛôÐö/ÁCµÅ>ñcáf«ð÷MÖ$ÚjÓïòÔ>úº/#$â½ö¢ý¨¬?gé)¤Mâ]w[¹û.¥Û>ß9¸ËÁ8å@À$zÿiOrè¾ÑÅÚZë^Ó.îÑ¬ZÐÞ0bó,°õï_BþÓ?¾|CøkðãÂþ,ñü^ñ41@¾ÕËìi<´]æ2sµSF9õÆtp¾Ò£½í{|¯¯©¤*×äoUµíúçìÁûKë_ßÄwüªø'ZÑdÎQ»[¹?ÂpãWÐjÙÃö²øÝã_-·ãðô¾½[ynRád7Ìd7vÜgß{¯â²ÏÆß~ý£ï¾øó\\´ñ´öÒKk[òD1"p`W,5Wþ	ÛÇÆ?Ú[þÂñÿéEõML4bª¾[Y&¬ßV8W5~éüÚoü£]ñþµ¢x?àæâióºywfxÄ(ÅL²+Ès½¥~Ê´ÖûHøYþÂ~;ÐÕòIØÎ
                C¡Ná03Íy'üæÖ!|d¹Ø¾yÕ3'}¡¥8üÉª_³Î¡oáÿø(7ÇÙ³¥ÜòìùVXXàw=kj´hÞ8BÎ);ßÓüÈ§Z¯îç9]Iµksø»áÿüyf¶Úî£¡pC§Ñü
                ké:E§[ØX[Çign"1UyÏÃß¾:ñöýÖRZÝ=0É²d]æ7ù@
                ·°àóÓ>§+Ã´PN¯FÜ£¸Ú)ÙVFË°·t_h^1F²ûw:rJ<×sl<üÌqøWmû[|3ð¿Äoú·ü$z<£ið½Í¤WL}åe #8=è¢½*²ktõ¹çÓIál×Cà_ØáücñvêMoCT:aZ-Ë»$nWví_bÁ@>xsÅÿ®õM_JóQÒ=Ñ,B[ÁR2¡Èö¢õqöû~+êÓ£<ûþ	ðÛÃZoµOÛé0¯'Ú½û3<WhÉ!Fzààf»ßÙ;Á'¾ üd¹Òì¾Ë>£~²]?ïæ7tsbÞ=1Ö+&êV»íùáÒä¥óö+ðNàûïH²û!»ÔVI¿zï½²ÜüÌqÔôªß	þxzÚ»âf¦ºrÝNÚx®äyªÏàT¶Ñ`(¢³~Ò¯¢ý
                b%?Vvß´|kâ)ÒÜ	m¦e÷rIñÆöËcâ½ç(¢¼êÍ¹jwQøEÆh¢ÀØÿÙ""";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "image/jpeg");
        mvm.add("Content-Length", "2322");
//        mvm.add("Content-disposition", "attachment; filename=\"wJrNmWwLYiQKHdIu65GZFup0iAtm2u2sPU4O5ESsDkx9dqhmgY3cOORAlx5ayWhs.jpg\"");
        mvm.add("Content-Disposition", "attachment; filename=\"wJrNmWwLYiQKHdIu65GZFup0iAtm2u2sPU4O5ESsDkx9dqhmgY3cOORAlx5ayWhs.jpg\"");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(printableText.getBytes(StandardCharsets.ISO_8859_1))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        webClient.get().uri("mp/asset/wJrNmWwLYiQKHdIu65GZFup0iAtm2u2sPU4O5ESsDkx9dqhmgY3cOORAlx5ayWhs?permanent=false")
                .exchange()
                .expectStatus().isOk()
//                .expectHeader().contentType(MediaType.IMAGE_JPEG)
                .expectHeader().contentDisposition(ContentDisposition.attachment().filename("wJrNmWwLYiQKHdIu65GZFup0iAtm2u2sPU4O5ESsDkx9dqhmgY3cOORAlx5ayWhs.jpg").build())
                .expectBody().consumeWith(x -> Assertions.assertEquals(FileType.JPG, InputStreamUtil.lookup(x.getResponseBody())));
    }

    @Test
    @DisplayName("下载永久素材")
    public void testDownloadMaterial() {
        // {"errcode":40007,"errmsg":"invalid media_id hint: [Y2XIpa0119p502] rid: 6413208f-1ae7191d-6edfb203"}

        String partOhHex = """
                4944330400000000003f5444524300000012000003323032312d31302d31322032303a313300545353450000000f0000034c61766635372e34312e3130300000000000000000000000fffb50c40000086400ef0084600144a4e0168a2000712559fb7945002f502071f1808067623100209a040eca0c7540839f18184e180c6200413282073e5063bfb3b1188010ca081cf9418ea81038f840309c100c620102751009200ee4ba31dcefb909d2f4aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa""";

        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
//        mvm.add("Content-disposition", "attachment; filename=\"1949FoundingCeremony.mp3\""); // 服务端自动处理响应头
        mvm.add("Content-Disposition", "attachment; filename=\"1949FoundingCeremony.mp3\"");
        mvm.add("Content-Length", "127132");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(partOhHex.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        webClient.get().uri("mp/asset/BJvvIFQ7gP1o40JR7X1fFrxu0HqLSRZf_r5T87rPPVzzR-pyBWDnQeLvx-keMwCm?permanent=true")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/octet-stream")
                .expectHeader().contentDisposition(ContentDisposition.attachment().filename("1949FoundingCeremony.mp3").build());
    }

    @Test
    @DisplayName("获取临时素材视频链接")
    public void testGetTemporalVideoUrl() {
        String textBody = "{\"video_url\":\"http://203.205.137.89/vweixinp.tc.qq.com/1007_9368551e5f9f412eb6bc703719e42a97.f10.mp4?vkey=54935E42F355FCA84FEB9B8E282D5C89765654165042A48DBB8D5B767E99892486DC0E37AAF7DE9A9EAAEE39BFCF15A6B90EFF92189B83E29AA2B84305287CFF11C09AC92C5FD6B9CBCBE72BB15545A16C39418E1FCA9817&sha=0&save=1\"}";

        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "297");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        webClient.get().uri("mp/asset/r5rrKN_03d9I9Nj5wek3_GGn3JmtHP8_nV_6v7Erh-DnX__xoGlfaeWY4xx7gEGA?permanent=false")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody().jsonPath("$.url").exists()
                .jsonPath("$.title").doesNotExist();
    }

    @Test
    @DisplayName("获取永久素材视频链接")
    public void testGetPermanentVideoUrl() {
        String textBody = "{\"title\":\"think\",\"description\":\"\",\"down_url\":\"http:\\/\\/203.205.137.71\\/vweixinp.tc.qq.com\\/1007_7fce7630998547f79359aaeed3ff46da.f10.mp4?vkey=29DA0D7BBD77BFC8C5F3DDAFF71E8CFD4500279E92366B673C590B6819058783631FA5A128626FD8FF059BF538969BC99484DD78A047519845999F67B78C411DF5CB962B5F4A15F2688D549BE77F6604642453A6FA161D36&sha=0&save=1\",\"newcat\":\"\",\"newsubcat\":\"\",\"tags\":[],\"cover_url\":\"\",\"vid\":\"1007_7fce7630998547f79359aaeed3ff46da\"}";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "431");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        webClient.get().uri("mp/asset/BJvvIFQ7gP1o40JR7X1fFl1KmdrbvveGwXe7cs1nM-niWtR8CHOJEHKFpV45sqMK?permanent=true")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.url").exists()
                .jsonPath("$.title").exists()
                .jsonPath("$.description").exists();
    }

    @Test
    @DisplayName("获取永久素材图文消息")
    public void testGetNews() {
        String textBody = "0a83010a4044332d2d547055346b4d7a5f4a42505365384c4e577a725552366e58646877643674714e4365646f3350786d737679653055304f6650536d5f4c7a75424467391224e6b58be8af95e7bea4e58f91e794a8e59bbee69687e6b688e681afe79a84e6a087e9a29822133c703e48656c6c6f20576f726c64213c2f703e380040004800109dc2d7af08";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
//        mvm.add("Content-disposition", "attachment; filename=\"1.jpg\"");
        mvm.add("Content-Disposition", "attachment; filename=\"1.jpg\"");
        mvm.add("Content-Length", "140");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        webClient.get().uri("mp/asset/D3--TpU4kMz_JBPSe8LNW97tQq_gAbOgHrXVwAvv2IgcVJrrEkA-OmNh0BUH-sub?permanent=true")
                .exchange()
                .expectStatus().isOk()
//                .expectHeader().contentType("application/json")
//                .expectBody()
//                .jsonPath("$.items").isArray()
        ;
    }

    @Test
    public void testUploadTempPicture() {
        String textBody = "{\"type\":\"thumb\",\"thumb_media_id\":\"N8hrPcMJtvCV2ZMMifpkEmxQzEKa0oI3Mc724ctF-hL0un9y8jBeWOdVKufPv6c0\",\"created_at\":1678599624,\"item\":[]}";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "134");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ClassPathResource("assets/resin_logo_small.jpg"), MediaType.IMAGE_PNG).filename("resin_logo_small.jpg");
        webClient.post().uri("mp/asset?permanent=false")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.createdAt").exists();
    }

    @Test
    public void testUploadTempVideo() {
        String textBody = "{\"type\":\"video\",\"media_id\":\"r5rrKN_03d9I9Nj5wek3_GGn3JmtHP8_nV_6v7Erh-DnX__xoGlfaeWY4xx7gEGA\",\"created_at\":1678600736,\"item\":[]}";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "128");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ClassPathResource("assets/20180726_164d6066636_r34.mp4"), MediaType.APPLICATION_OCTET_STREAM).filename("1964第一颗原子弹爆炸成功.mp4");
        webClient.post().uri("mp/asset?permanent=false")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.created").doesNotExist();
    }

    @ParameterizedTest
    @ValueSource(strings = {"BJvvIFQ7gP1o40JR7X1fFkfMtFIwPH-lDun_28LVZtv4Hlik3wFjSu6jVH4lzJYB",
            "BJvvIFQ7gP1o40JR7X1fFrxu0HqLSRZf_r5T87rPPVzzR-pyBWDnQeLvx-keMwCm",
            "BJvvIFQ7gP1o40JR7X1fFjx046v_4bUWEtPDETawG89eht_7y4WqErBcq54YS8RQ",
            "BJvvIFQ7gP1o40JR7X1fFm4RVya9eAEvt1KhscbHlCSYI4btk7TuE85nc4dwNfvw"})
    public void testDeletePermanentMaterial(String mediaId) {
        String textBody = "{\"errcode\":0,\"errmsg\":\"ok\"}";

        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "27");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        webClient.delete().uri("mp/asset/{mediaId}", mediaId)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testCountPermanentMaterial() {
        String textBody = "{\"voice_count\":0,\"video_count\":2,\"image_count\":12,\"news_count\":0}";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "65");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        webClient.get().uri("mp/asset")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.video").isNumber()
                .jsonPath("$.image").isNumber()
                .jsonPath("$.voice").isNumber()
                .jsonPath("$.news").isNumber();
    }

    @Test
    @DisplayName("分页获取永久素材列表")
    public void testGetPermanentMaterialDetail() {
        String textBody = "{\"item\":[{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFjQQgf54j1__QYE_Cht4VeeOytPUVJy3Byig2RAp_FUO\",\"name\":\"centos.png\",\"update_time\":1678525412,\"url\":\"https:\\/\\/mmbiz.qpic.cn\\/mmbiz_png\\/MDXeetibImLXOyjccFrzv24cfrR4wlFDyTEVcDnSNMusdkDLiacSXDiatcl6W4jPXBiav8B8vy8I6eT3tiaPicRJzPcA\\/0?wx_fmt=png\",\"tags\":[]},{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFnVsg68sdkXTxOVMQzH1vck1PnsTyAhTU_9hWcRFjvDW\",\"name\":\"centos.png\",\"update_time\":1671019130,\"url\":\"https:\\/\\/mmbiz.qpic.cn\\/mmbiz_png\\/MDXeetibImLUibIpWq7I9jcv9pVEhFUyib38uBx2klcsqpkI50ITjgpPyoVO8A9qBKcJicRUmN3ibEnXrPukeGq2w1Q\\/0?wx_fmt=png\",\"tags\":[]},{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFjN3BKW-poaK9XVJz-7lOyniZwLba0Z7sQaYBk04RKqO\",\"name\":\"centos.png\",\"update_time\":1670940673,\"url\":\"https:\\/\\/mmbiz.qpic.cn\\/mmbiz_png\\/MDXeetibImLUNR3mcNUICyuWHlwOu9iaTEsUXAGb3bUzqAH1YicDDkInCEOGyjx6C9oRraic8JkaFPMrxQg1cWlrqQ\\/0?wx_fmt=png\",\"tags\":[]},{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFjO56LW3jahnXxWO1K8KF-IWGo8DtEOXqGuiqFN7H8CH\",\"name\":\"centos.png\",\"update_time\":1670753589,\"url\":\"https:\\/\\/mmbiz.qpic.cn\\/mmbiz_png\\/MDXeetibImLXIvnibU9xcxRJpzWQyITP9eCDBHJNr9WUbSylLsyvico7iaW1sgkRIib1ZfhG2NaCCAhvHDc3BLrXTzQ\\/0?wx_fmt=png\",\"tags\":[]},{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFkYFeHBF3kzWBvMDS3wVECVj2A88Eu3Y43iWNTHZIakX\",\"name\":\"unix.gif\",\"update_time\":1670577109,\"url\":\"https:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"tags\":[]},{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFiH19FSiZivqJyYqDYhYK3X7e_gzDSxW1p6wpUNJixw-\",\"name\":\"unix.gif\",\"update_time\":1670574350,\"url\":\"https:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"tags\":[]},{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFqD-lsqR8V0GB1drS-5D8g5ZmSmSxoiw7rqKEkjZRid3\",\"name\":\"unix.gif\",\"update_time\":1670574272,\"url\":\"https:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"tags\":[]},{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFgR3KSr3WmDfcttyMCtjWAjBAa5w8KSzDDpYEmrqFGhA\",\"name\":\"unix.gif\",\"update_time\":1670574112,\"url\":\"https:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"tags\":[]},{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFpiU2PExXHSr985R0eXFUCOz1141R7PxGe4L5V68xRM3\",\"name\":\"unix.gif\",\"update_time\":1670573443,\"url\":\"https:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"tags\":[]},{\"media_id\":\"BJvvIFQ7gP1o40JR7X1fFnnz7DJYkySPFc70mSCJdxRVRAeOjggmbf-OEu7n0cnd\",\"name\":\"unix.gif\",\"update_time\":1670571682,\"url\":\"https:\\/\\/mmbiz.qpic.cn\\/mmbiz_gif\\/MDXeetibImLUlSdrZibMVYPIlfJqzw3t4zkHlTctickUlEZ8IXxGGiab1JZCyxG3Fh4343ITJt6FMBbhn29iaSnYg5g\\/0?wx_fmt=gif\",\"tags\":[]}],\"total_count\":12,\"item_count\":10}";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add("Content-Type", "text/plain");
        mvm.add("Content-Length", "965");
        mvm.add("Content-Encoding", "gzip");
        HttpHeaders httpHeaders = HttpHeaders.readOnlyHttpHeaders(mvm);

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(clientHttpResponse.getBody()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(textBody.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        webClient.get().uri("mp/asset?type=IMAGE&offset=0&limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isNumber()
                .jsonPath("$.items").isArray();
    }


}
