package weixin.mp.infrastructure.rpc;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import weixin.mp.domain.Context;
import weixin.mp.domain.FileType;
import weixin.mp.domain.MaterialType;
import weixin.mp.domain.MenuItem;
import weixin.mp.domain.MenuType;
import weixin.mp.domain.MessageType;
import weixin.mp.domain.ReplyMessage;
import weixin.mp.facade.WeixinApiFacade;
import weixin.mp.facade.WeixinArticleApiFacade;
import weixin.mp.facade.WeixinCustomServiceApiFacade;
import weixin.mp.facade.WeixinMaterialApiFacade;
import weixin.mp.facade.WeixinMenuApiFacade;
import weixin.mp.facade.WeixinMessageApiFacade;
import weixin.mp.facade.WeixinUserApiFacade;
import weixin.mp.facade.dto.ManualScript;
import weixin.mp.facade.dto.Material;
import weixin.mp.facade.dto.MessageReceipt;
import weixin.mp.facade.dto.MultiMedia;
import weixin.mp.facade.dto.Pageable;
import weixin.mp.facade.dto.Paper;
import weixin.mp.facade.dto.Publication;
import weixin.mp.infrastructure.cache.CacheKey;
import weixin.mp.infrastructure.exceptions.RetryableException;
import weixin.mp.infrastructure.exceptions.ServerError;
import weixin.mp.infrastructure.exceptions.WeixinExceptionUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;

public record Weixin(Context ctx, Cache cache, Lock lock) implements WeixinApiFacade, WeixinMaterialApiFacade,
        WeixinMenuApiFacade, WeixinArticleApiFacade, WeixinCustomServiceApiFacade, WeixinUserApiFacade,
        WeixinMessageApiFacade {

    private static final Logger log = LoggerFactory.getLogger(Weixin.class);

    private static final int MATERIAL_MAX_SIZE = 10 * 1024 * 1024;

    private static /* final */ WebClient httpClient;

    static {
        customizerWebClient(WebClient.builder());
    }

    private static final String ACCOUNT_FMT = "%1$s@%2$s";

    private static final int TRUE = 1;

    private static final int FALSE = 0;

    private static final String BOUNDARY_PARAM_NAME = "boundary";

    private static final String MEDIA_PARAM_NAME = "media";

    private static final String DESCRIPTION_NAME = "description";

    private static final byte[] BOUNDARY_MARK= {'-', '-'};

    private static final byte[] CARRIAGE_RETURN = {'\r', '\n'};

    private static final String CONTENT_DISPOSITION_WITHOUT_CARRIAGE_RETURN_FMT = "Content-Disposition: %1$s";

    private static final String CONTENT_TYPE_WITHOUT_CARRIAGE_RETURN_FMT = "Content-Type: %1$s";

    private static final String DESCRIPTION_FMT = "{\"title\":\"%1$s\",\"introduction\":\"%2$s\"}";

    /* Test use only */ static void customizerWebClient(WebClient.Builder builder) {
        httpClient = builder
                .codecs(clientCodecConfigurer -> {
                    clientCodecConfigurer.customCodecs()
                            .register(new Jackson2JsonDecoder(Jackson2ObjectMapperBuilder.json().build(),
                                    MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
                    clientCodecConfigurer.defaultCodecs().maxInMemorySize(MATERIAL_MAX_SIZE);
                })
                .build();
    }

    interface ServerResponse {
        int errorCode();
        String errorMessage();
        default boolean isSuccess() {
            return errorCode() == 0;
        }
    }

    /**
     * @param errorCode
     * @param errorMessage
     * @param accessToken 获取到的凭证
     * @param timeToLive 凭证有效时间，单位：秒
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthenticationResponse(@JsonAlias("errcode") int errorCode, @JsonAlias("errmsg") String errorMessage,
                                  @JsonAlias("access_token") @Size(min = 512) String accessToken,
                                  @JsonAlias("expires_in") int timeToLive) implements ServerResponse {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DefaultResponse(@JsonAlias("errcode") int errorCode,
                           @JsonAlias("errmsg") String errorMessage) implements ServerResponse {}

    private CompletableFuture<String> getOrRefreshAccessToken() {
        return getOrRefreshAccessToken(3);
    }

    private CompletableFuture<String> getOrRefreshAccessToken(final int times) {
        if (times == 0) {
            throw new RuntimeException("get access_token failure after tried %1$d times".formatted(times));
        }
        // 可以分层缓存，微信服务器支持刷新后5分钟内2个token同时可用
        AuthenticationResponse cached = cache.get(CacheKey.TOKEN_FMT.formatted(ctx.appId()), AuthenticationResponse.class);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached.accessToken());
        }
        boolean locked = false;
        try {
            locked = lock.tryLock(300, TimeUnit.MILLISECONDS);
            if (!locked) {
                return getOrRefreshAccessToken(times - 1);
            }
            return httpClient.get().uri(WeixinUrl.GET_ACCESS_TOKEN.getUrl(), ctx.appId(), ctx.appSecret())
                    .retrieve()
                    .bodyToMono(AuthenticationResponse.class)
                    .flatMap(r -> {
                        if (r.isSuccess()) {
                            cache.put(CacheKey.TOKEN_FMT.formatted(ctx.appId()), r);
                            return Mono.just(r.accessToken());
                        } else {
                            return Mono.error(new ServerError(r.errorCode(), "get access_token fail: errcode=%1$d, errmsg=%2$s".formatted(r.errorCode, r.errorMessage), HttpStatus.INTERNAL_SERVER_ERROR));
                        }
                    }).toFuture();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
        return getOrRefreshAccessToken(times - 1);
    }

    @Override
    public CompletableFuture<? extends GeneratedQuickResponseResult> generateQuickResponseCode(Integer ttl, Integer sceneId, String sceneMark) {
        QuickResponseAction actionName = QuickResponseAction.getInstance(ttl != null, sceneId != null);
        Function<String, CompletableFuture<GenerateQRResponse>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.RETRIEVE_QUICK_RESPONSE_CODE_TICKET.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GenQRCodeRequest(ttl, actionName.name(), new SceneWrapper(new Scene(sceneId, sceneMark))))
                .retrieve()
                .bodyToMono(GenerateQRResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param ttl 该二维码有效时间，以秒为单位。 最大不超过2592000（即30天），此字段如果不填，则默认有效期为60秒。
     * @param action 二维码类型，QR_SCENE为临时的整型参数值，QR_STR_SCENE为临时的字符串参数值，QR_LIMIT_SCENE为永久的整型参数值，QR_LIMIT_STR_SCENE为永久的字符串参数值
     * @param scene 二维码详细信息
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record GenQRCodeRequest(@JsonProperty("expire_seconds") Integer ttl, @JsonProperty("action_name") String action,
                            @JsonProperty("action_info") SceneWrapper scene) {}

    record SceneWrapper(Scene scene) {}

    /**
     * @param ticket 获取的二维码ticket，凭借此ticket可以在有效时间内换取二维码
     * @param ttl 该二维码有效时间，以秒为单位。 最大不超过2592000（即30天）
     * @param url 二维码图片解析后的地址，开发者可根据该地址自行生成需要的二维码图片
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateQRResponse(@JsonAlias("errcode") int errorCode, @JsonAlias("errmsg") String errorMessage,
                              String ticket,
                              @JsonAlias("expire_seconds") Integer ttl,
                              String url) implements ServerResponse, GeneratedQuickResponseResult {}

    /**
     * @param sceneId 场景值ID，临时二维码时为32位非0整型，永久二维码时最大值为100000（目前参数只支持1--100000）
     * @param sceneMark 场景值ID（字符串形式的ID），字符串类型，长度限制为1到64
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record Scene(@JsonProperty("scene_id") Integer sceneId, @JsonProperty("scene_str") String sceneMark) {}

    enum QuickResponseAction {
        QR_SCENE(true, true),
        QR_STR_SCENE(true, false),
        QR_LIMIT_SCENE(false, true),
        QR_LIMIT_STR_SCENE(false, false),
        ;
        private final boolean temporal;
        private final boolean numeric;
        QuickResponseAction(boolean temporal, boolean numeric) {
            this.temporal = temporal;
            this.numeric = numeric;
        }
        public static QuickResponseAction getInstance(boolean temporal, boolean numeric) {
            for (QuickResponseAction instance : QuickResponseAction.values()) {
                if (instance.temporal == temporal && instance.numeric == numeric) {
                    return instance;
                }
            }
            return null; // could not here
        }
    }

    @Override
    public CompletableFuture<InputStream> downloadQuickResponseCode(String ticket) {
        return httpClient.get()
                .uri(WeixinUrl.RETRIEVE_QUICK_RESPONSE_CODE.getUrl(), ticket)
                .retrieve()
                .bodyToMono(Resource.class)
                .map(r -> {
                    try {
                        return r.getInputStream();
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                })
                .toFuture();
    }

    @Override
    public CompletableFuture<String> shorten(String original, int ttl) {
        Function<String, CompletableFuture<String>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.GEN_SHORT_URL.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ShortenRequest(original, ttl))
                .retrieve()
                .bodyToMono(ShortenResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r.shortUrl()) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param data 需要转换的长信息，不超过4KB
     * @param ttl 过期秒数，最大值为2592000（即30天），默认为2592000
     */
    record ShortenRequest(@JsonProperty("long_data") String data, @JsonProperty("expire_seconds") Integer ttl) {}

    /**
     * @param shortUrl 短key，15字节，base62编码(0-9/a-z/A-Z)
     */
    record ShortenResponse(@JsonAlias("errcode") int errorCode, @JsonAlias("errmsg") String errorMessage,
                           @JsonAlias("short_key") String shortUrl) implements ServerResponse {}

    @Override
    public CompletableFuture<? extends OriginalResult> restore(String shortUtl) {
        Function<String, CompletableFuture<RestoreShortKeyResponse>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.RESTORE_SHORT_URL.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RestoreShortKeyRequest(shortUtl))
                .retrieve()
                .bodyToMono(RestoreShortKeyResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param shortKey 短key
     */
    record RestoreShortKeyRequest(@JsonProperty("short_key") String shortKey) {}

    /**
     * @param originalData 长信息
     * @param createdTime 创建的时间戳
     * @param ttl 剩余的过期秒数
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RestoreShortKeyResponse(@JsonAlias("errcode") int errorCode, @JsonAlias("errmsg") String errorMessage,
                                   @JsonAlias("long_data") String originalData,
                                   @JsonAlias("create_time") Long createdTime,
                                   @JsonAlias("expire_seconds") int ttl) implements ServerResponse, OriginalResult {
        @Override
        public LocalDateTime createdAt() {
            return createdTime != null ? LocalDateTime.ofEpochSecond(createdTime * 1000, 0, ZoneOffset.UTC) : null;
        }
    }

    @Override
    public CompletableFuture<Void> changeIndustry(String primaryIndustry, String secondaryIndustry) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.SET_INDUSTRY.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new IndustryRequest(primaryIndustry, secondaryIndustry))
                .retrieve()
                .bodyToMono(RestoreShortKeyResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param primaryIndustry 公众号模板消息所属行业编号
     * @param secondaryIndustry  公众号模板消息所属行业编号
     */
    record IndustryRequest(@JsonProperty("industry_id1") String primaryIndustry, @JsonAlias("industry_id2") String secondaryIndustry) {}

    @Override
    public CompletableFuture<? extends CorpOperation> viewIndustry() {
        Function<String, CompletableFuture<GetIndustryResponse>> curl = (accessToken) -> httpClient.get()
                .uri(WeixinUrl.GET_INDUSTRY.getUrl(), accessToken)
                .retrieve()
                .bodyToMono(GetIndustryResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param main 帐号设置的主营行业
     * @param side 帐号设置的副营行业
     */
    record GetIndustryResponse(@JsonAlias("errcode") int errorCode, @JsonAlias("errmsg") String errorMessage,
                               @JsonAlias("primary_industry") IndustryResult main,
                               @JsonAlias("secondary_industry") IndustryResult side) implements ServerResponse, CorpOperation {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IndustryResult(@JsonAlias("first_class") String primaryIndustryDescription,
                          @JsonAlias("second_class") String secondaryIndustryDescription) implements IndustryDescription {}




    // 素材




    WebClient.RequestHeadersSpec multipart(WebClient.RequestBodySpec spec, InputStream is, String name, String filename) {
        return multipart(spec, is, name, filename, null);
    }

    /**
     * 微信服务端在"Content-Type"为"multipart/form-data"格式进行传输时要求"Content-Length"必须正确。
     * Disables 'Transfer-Encoding: chunked' avoid server response with status 412
     * @see reactor.netty.http.client.HttpClientConnect.HttpClientHandler#requestWithBody 803-->991
     * @param spec 已填充url的请求
     * @param is 流
     * @param name 参数名称
     * @param filename 文件名称
     * @param description 视频描述
     * @return 已处理"multipart-form-data"的请求
     */
    WebClient.RequestHeadersSpec multipart(WebClient.RequestBodySpec spec, InputStream is, String name, String filename, VideoDescription description) {
        int streamAvailable = 0;
        try {
            streamAvailable = is.available();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        byte[] boundary = MimeTypeUtils.generateMultipartBoundary();
        String fileContentDisposition = ContentDisposition.formData().name(name).filename(filename).build().toString();
        byte[] contentDisposition = CONTENT_DISPOSITION_WITHOUT_CARRIAGE_RETURN_FMT.formatted(fileContentDisposition).getBytes(StandardCharsets.UTF_8);
        byte[] file = new byte[0];
        try {
            file = is.readAllBytes();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RetryableException(e.getMessage(), e);
        }
        String mediaType = fileType2mediaType(is.markSupported() ? InputStreamUtil.lookup(is) : InputStreamUtil.lookup(file)) ;
        byte[] contentType = CONTENT_TYPE_WITHOUT_CARRIAGE_RETURN_FMT.formatted(mediaType).getBytes(StandardCharsets.US_ASCII);
        Flux<byte[]> body = Flux.concat(Flux.just(BOUNDARY_MARK), Flux.just(boundary), Flux.just(CARRIAGE_RETURN), // first boundary
                Flux.just(contentDisposition), Flux.just(CARRIAGE_RETURN), // Content-Disposition
                Flux.just(contentType), Flux.just(CARRIAGE_RETURN), // Content-Type
                Flux.just(CARRIAGE_RETURN),
                Flux.just(file), Flux.just(CARRIAGE_RETURN),// data
                Flux.just(BOUNDARY_MARK), Flux.just(boundary)); // part of boundary, missing '\r\n' as middle boundary or '--\r\n' as last boundary
        int contentLength = CARRIAGE_RETURN.length * 5 + BOUNDARY_MARK.length * 2 + boundary.length * 2
                + contentDisposition.length + contentType.length + streamAvailable;
        if (description != null) {
            String nextContentDisposition = ContentDisposition.formData().name(DESCRIPTION_NAME).build().toString();
            byte[] descriptionContentDisposition = CONTENT_DISPOSITION_WITHOUT_CARRIAGE_RETURN_FMT.formatted(nextContentDisposition).getBytes(StandardCharsets.US_ASCII);
            // Content-Type ignore
            byte[] descriptionData = DESCRIPTION_FMT.formatted(description.title(), description.introduction()).getBytes(StandardCharsets.UTF_8);
            Flux<byte[]> lastBoundary = Flux.concat(Flux.just(BOUNDARY_MARK), Flux.just(boundary), Flux.just(BOUNDARY_MARK), Flux.just(CARRIAGE_RETURN));
            body = body.concatWithValues(CARRIAGE_RETURN, descriptionContentDisposition, CARRIAGE_RETURN, CARRIAGE_RETURN, descriptionData, CARRIAGE_RETURN).concatWith(lastBoundary);
            contentLength += descriptionContentDisposition.length + descriptionData.length + CARRIAGE_RETURN.length * 5 + BOUNDARY_MARK.length * 2 + boundary.length;
        } else {
            body = body.concatWithValues(BOUNDARY_MARK, CARRIAGE_RETURN); // concat as last boundary
            contentLength += BOUNDARY_MARK.length + CARRIAGE_RETURN.length;
        }
        Map<String, String> params = new HashMap<>();
        params.put(BOUNDARY_PARAM_NAME, new String(boundary, StandardCharsets.US_ASCII));
        return spec.contentLength(contentLength)
                .contentType(new MediaType(MediaType.MULTIPART_FORM_DATA, params))
                .body(body, byte[].class);
    }

    private String fileType2mediaType(FileType fileType) {
        if (fileType != null) {
            switch (fileType) {
                case PNG: return MediaType.IMAGE_PNG_VALUE;
                case JPG:
                case JPEG:
                    return MediaType.IMAGE_JPEG_VALUE;
                case GIF:
                    return MediaType.IMAGE_GIF_VALUE;
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private MaterialType fileType2materialType(FileType fileType, InputStream is) {
        if (fileType != FileType.JPG) {
            return MaterialType.getInstance(fileType);
        }
        try {
            int size = is.available();
            return size <= MaterialType.THUMB.getMaxSize() * 1024 ? MaterialType.THUMB : MaterialType.IMAGE;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public CompletableFuture<? extends Material> upload(InputStream is, String filename, boolean forever, VideoDescription description) {
        // TODO 检查历史是否上传过，即给文件生成指纹进行判断
        String uriTemplate = forever ? WeixinUrl.UPLOAD_PERMANENT_MATERIAL.getUrl() : WeixinUrl.UPLOAD_MATERIAL.getUrl();
        Class responseClass = forever ? PermanentMaterialResponse.class : TemporalMaterialResponse.class;
        FileType fileType = is.markSupported() ? InputStreamUtil.lookup(is) : FileType.getInstance(filename.substring(filename.lastIndexOf(".") + 1));
        MaterialType materialType = fileType2materialType(fileType, is);
        Function<String, CompletableFuture<Material>> curl = (accessToken) ->
                multipart(httpClient.post().uri(uriTemplate, accessToken, materialType.getValue()), is, MEDIA_PARAM_NAME, filename, description)
                .retrieve()
                .bodyToMono(responseClass)
                .flatMap(r -> ((ServerResponse) r).isSuccess() ?
                        Mono.just(r) :
                        Mono.error(WeixinExceptionUtil.create(((ServerResponse) r).errorCode(), ((ServerResponse) r).errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<? extends NewsResult> addNews(List<? extends ManualScript> news, boolean broadcastPurpose) {
        // TODO 群发则检查公众号是否认证通过；缩略图如果群发则为临时mediaId，否则为永久mediaId
        final String urlTemplate = broadcastPurpose ? WeixinUrl.UPLOAD_ARTICLE.getUrl() : WeixinUrl.ADD_PERMANENT_NEWS.getUrl();
        Function<String, CompletableFuture<UploadNewsResponse>> curl = (accessToken) -> httpClient.post()
                .uri(urlTemplate, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddNewsRequest(news.stream().map(ArticleItem::from).collect(Collectors.toList())))
                .retrieve()
                .bodyToMono(UploadNewsResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    record AddNewsRequest(List<ArticleItem> articles) {}

    /**
     * @param materialType 群发用的图文消息返回固定的news
     * @param id 媒体id
     * @param createTime 媒体文件上传时间
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record UploadNewsResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg")  String errorMessage,
                              @JsonAlias("type") String materialType,
                              @JsonAlias("media_id") String id,
                              @JsonAlias("created_at") Long createTime) implements NewsResult, ServerResponse {
        @Override
        public MaterialType type() {
            return MaterialType.getInstance(materialType);
        }
        @Override
        public LocalDateTime createdAt() {
            return createTime != null ? LocalDateTime.ofEpochSecond(createTime * 1000, 0, ZoneOffset.UTC) : null;
        }
    }

    /**
     * @param id 新增的永久素材的media_id
     * @param url 新增的图片素材的图片URL（仅新增图片素材时会返回该字段）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PermanentMaterialResponse(@JsonProperty("errcode") int errorCode,
                                     @JsonProperty("errmsg") String errorMessage,
                                     @JsonAlias(value = "media_id") String id,
                                     String url) implements ServerResponse, PermanentMaterial {}

    /**
     * @param materialType 媒体文件类型
     * @param id 媒体文件上传后的标识
     * @param createTime 媒体文件上传时间戳
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record TemporalMaterialResponse(@JsonProperty("errcode") int errorCode,
                                    @JsonProperty("errmsg") String errorMessage,
                                    @JsonProperty("type") String materialType,
                                    @JsonAlias(value = {"media_id", "thumb_media_id"}) String id,
                                    @JsonAlias("created_at") Long createTime) implements ServerResponse, TemporalMaterial {
        @Override
        public MaterialType type() {
            return MaterialType.getInstance(materialType);
        }
        @Override
        public LocalDateTime createdAt() {
            if (createTime != null) {
                return LocalDateTime.ofEpochSecond(createTime * 1000, 0, ZoneOffset.UTC);
            }
            return null;
        }
    }

    @Override
    public CompletableFuture<String> uploadImage(InputStream is, String filename) {
        // TODO 检查公众号是否认证通过
        FileType fileType = is.markSupported() ? InputStreamUtil.lookup(is) : FileType.getInstance(filename.substring(filename.lastIndexOf(".") + 1));
        assert fileType == FileType.JPG || fileType == FileType.PNG;
        MaterialType materialType = fileType2materialType(fileType, is);
        Function<String, CompletableFuture<String>> curl = (accessToken) ->
                multipart(httpClient.post().uri(WeixinUrl.UPLOAD_IMG.getUrl(), accessToken, materialType.getValue()), is, MEDIA_PARAM_NAME, filename, null)
                        .retrieve()
                        .bodyToMono(UploadImageResponse.class)
                        .flatMap(r -> r.isSuccess() ?
                                Mono.just(r.url()) :
                                Mono.error(WeixinExceptionUtil.create(((ServerResponse) r).errorCode(), ((ServerResponse) r).errorMessage())))
                        .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UploadImageResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg")  String errorMessage,
                               String url) implements ServerResponse {}

    @Override
    public CompletableFuture<? extends Downloadable> download(String mediaId, boolean forever) {
        // TODO 检查永久素材是否上传成功，临时素材是否失效
        Function<String, WebClient.RequestHeadersSpec> spec = (accessToken) -> forever ?
                httpClient.post().uri(WeixinUrl.GET_PERMANENT_MATERIAL.getUrl(), accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new MediaRequest(mediaId)) :
                httpClient.post().uri(WeixinUrl.DOWNLOAD_MEDIA.getUrl(), accessToken, mediaId);
        Function<ClientResponse, ? extends Mono<Downloadable>> clientResponseHandler = (clientResponse) ->
                clientResponse.headers().asHttpHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION) ?
                        clientResponse.bodyToMono(Resource.class) // 图片、缩略图、音频直接返回流
                                .map(r -> {
                                    try {
                                        return new DownloadableStreamMaterialResponse(r.getFilename(), r.getInputStream());
                                    } catch (Exception e) {
                                        log.error(e.getMessage(), e);
                                        throw new RuntimeException(e);
                                    }
                                })
                        :
                        clientResponse.bodyToMono(DownloadableMaterialResponse.class) // 图文、视频返回json结构
                                .flatMap(r -> r.isSuccess() ?
                                        Mono.just((Downloadable) r) :
                                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())));
        Function<String, CompletableFuture<DownloadableStreamMaterialResponse>> curl =(accessToken) -> spec.apply(accessToken)
                .exchangeToMono(clientResponseHandler)
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param title 永久素材视频标题
     * @param description 永久素材视频描述
     * @param url 永久/临时素材视频下载url
     * @param items 永久素材的图文清单
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record DownloadableMaterialResponse(@JsonProperty("errcode") int errorCode,
                                @JsonProperty("errmsg") String errorMessage,
                                String title, String description,
                                @JsonAlias(value = {"down_url", "video_url"}) String url,
                                @JsonAlias(value = {"news_item"}) List<ArticleItem> items) implements ServerResponse,
            WeixinMaterialApiFacade.News, WeixinMaterialApiFacade.Video {}

    record DownloadableStreamMaterialResponse(String filename, InputStream body) implements WeixinMaterialApiFacade.Stream {
    }

    /**
     * @param title 图文消息的标题
     * @param thumbMediaId 图文消息的封面图片素材id（永久素材mediaID，群发时可以为临时mediaId）
     * @param showCoverPicture 是否显示封面，0为false，即不显示，1为true，即显示
     * @param author 作者
     * @param digest 图文消息的摘要，仅有单图文消息才有摘要，多图文此处为空
     * @param content 图文消息的具体内容，支持HTML标签，必须少于2万字符，小于1M，且此处会去除JS
     * @param url 图文页的URL
     * @param contentSourceUrl 图文消息的原文地址，即点击“阅读原文”后的URL
     *
     * @param commentState 是否打开评论，0不打开(默认)，1打开。<b>仅草稿、发布接口返回该字段</b>
     * @param fansCommentState 是否粉丝才可评论，0所有人可评论(默认)，1粉丝才可评论。<b>仅草稿、发布接口返回该字段</b>
     * @param deleted 该图文是否被删除(<b>仅发布后查询接口返回该字段</b>)
     *
     * @param coverUrl 封面图片的URL(仅作为公众号管理后台设置的菜单时返回)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record ArticleItem(String title,
                       @JsonProperty("thumb_media_id") String thumbMediaId,
                       @JsonProperty("show_cover_pic") @JsonAlias(value = {"show_cover_pic", "show_cover"}) Integer showCoverPicture,
                       @JsonAlias(value = "cover_url") String coverUrl,
                       String author, String digest, String content, @JsonAlias(value = {"content_url"}) String url,
                       @JsonProperty(value = "content_source_url") String contentSourceUrl,
                       @JsonProperty("need_open_comment") Integer commentState,
                       @JsonProperty("only_fans_can_comment") Integer fansCommentState,
                       @JsonAlias("is_deleted") Boolean deleted) implements Paper {
        public static ArticleItem from(ManualScript paper) {
            return new ArticleItem(paper.title(), paper.thumbMediaId(),
                    paper.displayCover() ? TRUE : FALSE, null, paper.author(), paper.digest(),
                    paper.content(), null, paper.contentSourceUrl(), paper.commentEnabled() ? TRUE : FALSE,
                    paper.onlyFansComment() ? TRUE : FALSE, null);
        }
        @Override
        public boolean displayCover() {
            return showCoverPicture != null && showCoverPicture == TRUE;
        }
        @Override
        public boolean commentEnabled() {
            return commentState != null && commentState == TRUE;
        }
        @Override
        public boolean onlyFansComment() {
            return fansCommentState != null && fansCommentState == TRUE;
        }
    }

    @Override
    public CompletableFuture<Void> recycle(String mediaId) {
        // TODO 检查永久素材是否存在
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.REMOVE_PERMANENT_MATERIAL.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new MediaRequest(mediaId))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r ->  r.isSuccess() ? Mono.<Void>empty() : Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<? extends MaterialDistribution> countMaterialByType() {
        Function<String, CompletableFuture<MaterialDistributionResponse>> curl = (accessToken) -> httpClient.get().
                uri(WeixinUrl.COUNT_PERMANENT_MATERAIL.getUrl(), accessToken)
                .retrieve()
                .bodyToMono(MaterialDistributionResponse.class)
                .flatMap(r ->  r.isSuccess() ? Mono.just(r) : Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param voice 语音总数量
     * @param video 视频总数量
     * @param image 图片总数量
     * @param news 图文总数量
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record MaterialDistributionResponse(@JsonProperty("errcode") int errorCode,
                                        @JsonProperty("errmsg") String errorMessage,
                                        @JsonAlias("voice_count") int voice,
                                        @JsonAlias("video_count") int video,
                                        @JsonAlias("image_count") int image,
                                        @JsonAlias("news_count") int news) implements MaterialDistribution, ServerResponse{}

    @Override
    public CompletableFuture<? extends Pageable<? extends Material>> list(MaterialType type, int offset, int limit) {
        Function<String, CompletableFuture<ListPermanentMaterialResponse>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.LIST_PERMANENT_MATERIAL.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ListPermanentMaterialRequest(type.getValue(), offset, limit))
                .retrieve()
                .bodyToMono(ListPermanentMaterialResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(new ListPermanentMaterialResponse(r.errorCode(), r.errorMessage(), r.total(), offset, limit, r.fetched(), r.items())) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param type 素材的类型
     * @param offset 从全部素材的该偏移位置开始返回，0表示从第一个素材 返回
     * @param count 返回素材的数量，取值在1到20之间
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record ListPermanentMaterialRequest(String type, int offset, int count) {}

    /**
     * @param total 该类型的素材的总数
     * @param fetched 本次调用获取的素材的数量
     * @param items 素材列表
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ListPermanentMaterialResponse(@JsonProperty("errcode") int errorCode,
                                         @JsonProperty("errmsg") String errorMessage,
                                         @JsonAlias("total_count") int total,
                                         int offset,
                                         int pageSize,
                                         @JsonAlias("item_count") int fetched,
                                         @JsonAlias("item") List<PermanentMaterialItem> items) implements ServerResponse,
            Pageable<PermanentMaterialItem> {}

    /**
     * @param id 媒体文件id/成功发布的图文消息id
     * @param updatedTime 最后更新时间
     *
     * @param name （图片、语音、视频）文件名称
     * @param url 图片的URL
     *
     * @param content 永久图文消息素材
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PermanentMaterialItem(@JsonAlias(value = {"media_id", "article_id"}) String id,
                                 @JsonAlias("update_time") Long updatedTime,
                                 String name,
                                 String url,
                                 @JsonAlias("content") NewsList content) implements MultiMedia, Publication {
        @Override
        public LocalDateTime updatedAt() {
            return updatedTime == null ? null : LocalDateTime.ofEpochSecond(updatedTime * 1000, 0, ZoneOffset.UTC);
        }
        @Override
        public List<? extends Paper> items() {
            return content == null ? Collections.emptyList() : content.newsItem();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NewsList(@JsonAlias("news_item") List<ArticleItem> newsItem) {}




    // 菜单




    @Override
    public CompletableFuture<String> create(List<? extends MenuItem> items, MenuMatchRule rule) {
        // TODO 检查article_id是否为已发布文章, 检查media_id是否为永久素材
        final String uriTemplate = rule == null ? WeixinUrl.CREATE_MENU.getUrl() : WeixinUrl.CREATE_CUSTOM_MENU.getUrl();
        List<MenuButton> buttons = new ArrayList<>(items.size());
        for (MenuItem item : items) {
            buttons.add(assemble(item));
        }
        Object body = rule == null ?
                new Menu(buttons, null) :
                new CustomMenu(buttons, new CustomMenuMatchRule(rule.tagId(), rule.clientPlatform()), null);
        Function<String, CompletableFuture<String>> curl = (accessToken) -> httpClient.post().
                uri(uriTemplate, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(CreateMenuResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.justOrEmpty(r.menuId()) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param menuId 仅创建个性化菜单时返回
     */
    record CreateMenuResponse(@JsonAlias("errcode") int errorCode, @JsonAlias("errmsg") String errorMessage,
                              @JsonAlias("menuid") String menuId) implements ServerResponse {}

    private MenuButton assemble(MenuItem item) {
        MenuButton button = null;
        if (item instanceof MenuItem.BoxItem box) {
            List<MenuButton> buttons = new ArrayList<>(box.items().size());
            for (MenuItem mi : box.items()) {
                buttons.add(assemble((mi)));
            }
            // no need serialize type which is sub_button , so leave it to null
            button = new MenuButton(box.name(), null, null, null, null, null, null, null, null, buttons);
        } else if (item instanceof MenuItem.KeyItem key) {
            button = new MenuButton(key.name(), key.kind().getValue(), null, key.key(), null, null, null, null, null, null);
        } else if (item instanceof MenuItem.ProgramletItem programlet) {
            button = new MenuButton(programlet.name(), programlet.kind().getValue(), null, null, programlet.url(), null, null, programlet.appId(), programlet.pagePath(), null);
        } else if (item instanceof MenuItem.ViewItem url) {
            button = new MenuButton(url.name(), url.kind().getValue(), null, null, url.url(), null, null, null, null, null);
        } else if (item instanceof MenuItem.ValueItem value) {
            if (item.kind() == MenuType.NEWS1) {
                button = new MenuButton(value.name(), value.kind().getValue(), null, null, null, value.value(), null, null, null, null);
            } else if (item.kind() == MenuType.NEWS2 || item.kind() == MenuType.NEWS3) {
                button = new MenuButton(value.name(), value.kind().getValue(), null, null, null, null, value.value(), null, null, null);
            } else {
                log.warn("API不允许菜单类型：{}", item.kind());
            }
        }
        return button;
    }

    /**
     * @param button 一级菜单数组，个数应为1~3个
     *
     * @param menuId 有个性化菜单时查询返回
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Menu(@JsonProperty("button") List<MenuButton> button, @JsonProperty("menuid") Long menuId) {}

    /**
     * @param button 一级菜单数组，个数应为1~3个
     * @param rule 菜单匹配规则
     *
     * @param menuId 有个性化菜单时查询返回
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record CustomMenu(List<MenuButton> button,
                      @JsonProperty("matchrule") CustomMenuMatchRule rule,
                      @JsonProperty("menuid") Long menuId) implements CustomMenuResult {
        @Override
        public List<? extends MenuItem> items() {
            return button;
        }
    }

    /**
     * @param tagId 用户标签的id，可通过用户标签管理接口获取
     * @param clientPlatform 客户端版本，当前只具体到系统型号：IOS(1), Android(2),Others(3)，不填则不做匹配
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record CustomMenuMatchRule(@JsonProperty("tag_id") String tagId,
                               @JsonProperty("client_platform_type") String clientPlatform) implements MenuMatchRule {}

    /**
     * @param buttonType 菜单的响应动作类型
     * @param name 菜单标题，不超过16个字节，子菜单不超过40个字节
     * @param key 菜单KEY值，用于消息接口推送，不超过128字节
     * @param url 网页链接，用户点击菜单可打开链接，不超过1024字节
     * @param mediaId 调用新增永久素材接口返回的合法media_id
     * @param articleId 发布后获得的合法 article_id
     * @param appId 小程序的appid
     * @param pagePath 小程序的页面路径
     * @param buttons 二级菜单数组，个数应为1~5个
     *
     * @param value 微信公众号管理后台设置的菜单值
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record MenuButton(String name,
                      @JsonProperty("type") String buttonType,
                      String value,
                      String key,
                      String url,
                      @JsonProperty(value = "media_id") String mediaId,
                      @JsonProperty(value = "article_id") String articleId,
                      @JsonProperty(value = "appid") String appId,
                      @JsonProperty(value = "pagepath") String pagePath,
                      @JsonProperty(value = "sub_button") List<MenuButton> buttons) implements MenuItem,
            MenuItem.BoxItem, MenuItem.ValueItem, MenuItem.KeyItem, MenuItem.ProgramletItem {
        @Override
        public MenuType kind() {
            return MenuType.getInstance(buttonType);
        }
        @Override
        public List<? extends MenuItem> items() {
            return buttons;
        }
        @Override
        public String getValue() {
            if (value != null) {
                return value;
            } else if (mediaId != null) {
                return mediaId;
            } else if (articleId != null) {
                return articleId;
            } else {
                return null;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record News(String name, String value, List<ArticleItem> list) {}

    @Override
    public CompletableFuture<? extends List<? extends MenuItem>> test(String userId) {
        Function<String, CompletableFuture<List<MenuButton>>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.TEST_CUSTOM_MENU.getUrl(), accessToken)
                .bodyValue(new TestCustomMenuRequest(userId))
                .retrieve()
                .bodyToMono(TestCustomMenuResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r.buttons()) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record TestCustomMenuRequest(@JsonProperty("user_id") String userId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TestCustomMenuResponse(@JsonAlias("errcode") int errorCode, @JsonAlias("errmsg") String errorMessage,
                                  @JsonAlias("button") List<MenuButton> buttons) implements ServerResponse {}

    @Override
    public CompletableFuture<MenuBar> get(boolean custom) {
        String uriTemplate = custom ? WeixinUrl.GET_CUSTOM_MENU.getUrl() : WeixinUrl.GET_MENU.getUrl();
        Class<? extends ServerResponse> responseType = custom ? CustomMenuResponse.class : MenuResponse.class;
        Function<String, CompletableFuture<MenuBar>> curl = (accessToken) -> httpClient.get().
                uri(uriTemplate, accessToken)
                .retrieve()
                .bodyToMono(responseType)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just((MenuBar) r) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MenuResultWrapper(@JsonAlias("button") List<MenuButtonItem> buttons) {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type",
            visible = true, defaultImpl = MenuSubButton.class)
    @JsonSubTypes(value = {@JsonSubTypes.Type(value = MenuLeafButton.class,
            names = {"news", "img", "voice", "video", "view", "miniprogram", "click", "scancode_push",
                    "scancode_waitmsg", "pic_sysphoto", "pic_weixin", "pic_photo_or_album", "location_select",
                    "media_id", "article_id", "article_view_limited"})})
    interface MenuButtonItem extends MenuItem {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MenuSubButton(String name,
                      @JsonAlias(value = {"sub_button"}) SubButtonWrapper buttons) implements MenuButtonItem,
            MenuItem.BoxItem {
        @Override
        public MenuType kind() {
            return MenuType.BOX;
        }
        @Override
        public List<? extends MenuItem> items() {
            return buttons != null ? buttons.list() : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SubButtonWrapper(List<MenuLeafButton> list) {}

    /**
     * @param name 菜单名称
     * @param buttonType 菜单的类型
     * @param value Text:保存文字到value； Img、voice：保存mediaID到value；Video：保存视频下载链接到value；news保存mediaID到value
     * @param key 使用API设置的自定义菜单保存值到key
     * @param url View：保存链接到url，miniprogram保存链接到url
     * @param appId miniprogram的id
     * @param pagePath miniprogram的页面路径
     * @param mediaId
     * @param articleId
     * @param news 图文消息的信息
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record MenuLeafButton(String name,
                          @JsonAlias("type") String buttonType,
                          String value,
                          String key,
                          String url,
                          @JsonAlias(value = {"mediaId", "media_id"}) String mediaId,
                          @JsonAlias(value = {"articleId", "article_id"}) String articleId,
                          @JsonAlias(value = {"appId", "appid"}) String appId,
                          @JsonAlias(value = {"pagePath", "pagepath"}) String pagePath,
                          @JsonAlias(value = {"news", "news_info"}) News news) implements MenuButtonItem,
            MenuItem.ValueItem, MenuItem.KeyItem, MenuItem.ViewItem, MenuItem.ProgramletItem, MenuItem.PaperItem {
        @Override
        public MenuType kind() {
            return MenuType.getInstance(buttonType);
        }
        @Override
        public String getValue() {
            return Optional.ofNullable(value).orElseGet(() -> Optional.ofNullable(mediaId).orElse(articleId));
        }
        @Override
        public List<? extends Paper> items() {
            return news != null ? news.list() : null;
        }
    }

    /**
     * @param menuOpen 菜单是否开启，0代表未开启，1代表开启
     * @param menu 菜单信息
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record MenuResponse(@JsonAlias("errcode") int errorCode, @JsonAlias("errmsg") String errorMessage,
                        @JsonAlias("is_menu_open") Integer menuOpen,
                        @JsonAlias("selfmenu_info") MenuResultWrapper menu) implements ServerResponse, MenuBar {
        @Override
        public MenuResult fixedMenu() {
            return new MenuResult() {
                @Override
                public List<? extends MenuItem> items() {
                    return menu.buttons();
                }

                @Override
                public Long menuId() {
                    return null;
                }

                @Override
                public boolean isEnabled() {
                    return menuOpen != null && menuOpen == TRUE;
                }
            };
        }
        @Override
        public List<? extends CustomMenuResult> conditionalMenus() {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CustomMenuResponse(@JsonAlias("errcode") int errorCode, @JsonAlias("errmsg") String errorMessage,
                              Menu menu,
                              @JsonAlias("conditionalmenu") List<CustomMenu> customMenus) implements ServerResponse, MenuBar {
        @Override
        public MenuResult fixedMenu() {
            return new MenuResult() {
                @Override
                public List<? extends MenuItem> items() {
                    return menu.button();
                }

                @Override
                public Long menuId() {
                    return menu.menuId();
                }

                @Override
                public boolean isEnabled() {
                    return true;
                }
            };
        }
        @Override
        public List<? extends CustomMenuResult> conditionalMenus() {
            return customMenus;
        }
    }

    @Override
    public CompletableFuture<Void> delete(Long menuId) {
        Function<String, WebClient.RequestHeadersSpec> spec = (accessToken) -> menuId == null ?
                httpClient.get().uri(WeixinUrl.DELETE_MENU.getUrl(), accessToken) :
                httpClient.post().uri(WeixinUrl.DELETE_CUSTOM_MENU.getUrl(), accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new Menu(null, menuId));
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> spec.apply(accessToken)
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }




    // 草稿、发布、留言




    @Override
    public CompletableFuture<String> draft(List<? extends ManualScript> draft) {
        // TODO 检查thumbMediaId是否为曾经上传过的永久素材mediaId
        List<DraftItem> articles = new ArrayList<>(draft.size());
        for (ManualScript article : draft) {
            articles.add(new DraftItem(article.title(), article.author(), article.digest(), article.content(),
                    article.contentSourceUrl(), article.thumbMediaId(),
                    article.commentEnabled() ? TRUE : FALSE,
                    article.onlyFansComment() ? TRUE : FALSE,
                    null, null));
        }
        Function<String, CompletableFuture<String>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.CREATE_DRAFT.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new DraftRequest(articles))
                .retrieve()
                .bodyToMono(MediaResponse.class)
                .flatMap(t -> t.isSuccess() ?
                        Mono.just(t.mediaId()) :
                        Mono.error(WeixinExceptionUtil.create(t.errorCode(), t.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record DraftRequest(List<DraftItem> articles) {}

    /**
     * @param title 标题
     * @param author 作者
     * @param digest 图文消息的摘要，仅有单图文消息才有摘要，多图文此处为空。如果本字段为没有填写，则默认抓取正文前54个字
     * @param content 图文消息的具体内容，支持HTML标签，必须少于2万字符，小于1M，且此处会去除JS,涉及图片url必须来源 "上传图文消息内的图片获取URL"接口获取。外部图片url将被过滤
     * @param contentSourceUrl 图文消息的原文地址，即点击“阅读原文”后的URL
     * @param thumbMediaId 图文消息的封面图片素材id（必须是永久MediaID）
     * @param commentOpenState 是否打开评论，0不打开(默认)，1打开
     * @param onlyFansCommentState 是否粉丝才可评论，0所有人可评论(默认)，1粉丝才可评论
     *
     * @param showCoverPicture 是否在正文显示封面（仅获取草稿时返回）
     * @param url 草稿的临时链接（仅获取草稿时返回）
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record DraftItem(@NotEmpty String title,
                     String author,
                     String digest,
                     @NotEmpty @Size(max = 20000) String content,
                     @JsonProperty("content_source_url") String contentSourceUrl,
                     @NotEmpty @JsonProperty("thumb_media_id") String thumbMediaId,
                     @JsonProperty("need_open_comment") Integer commentOpenState,
                     @JsonProperty("only_fans_can_comment") Integer onlyFansCommentState,
                     @Deprecated @JsonProperty("show_cover_pic") Integer showCoverPicture,
                     String url) implements Paper{
        @Override
        public boolean displayCover() {
            return showCoverPicture != null && showCoverPicture == TRUE;
        }
        @Override
        public boolean commentEnabled() {
            return commentOpenState != null && commentOpenState == TRUE;
        }
        @Override
        public boolean onlyFansComment() {
            return onlyFansCommentState != null && onlyFansCommentState == TRUE;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MediaResponse(@JsonProperty("errcode") int errorCode,
                         @JsonProperty("errmsg") String errorMessage,
                         @JsonProperty("media_id") String mediaId) implements ServerResponse {}

    record MediaRequest(@JsonProperty("media_id") String mediaId) {}

    @Override
    public CompletableFuture<? extends List<? extends Paper>> retrieve(String mediaId) {
        Function<String, CompletableFuture<List<DraftItem>>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.RETRIEVE_ONE_DRAFT.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new MediaRequest(mediaId))
                .retrieve()
                .bodyToMono(GetDraftResponse.class)
                .flatMap(t -> t.isSuccess() ?
                        Mono.just(t.articles()):
                        Mono.error(WeixinExceptionUtil.create(t.errorCode(), t.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    record GetDraftResponse(@JsonProperty("errcode") int errorCode,
                            @JsonProperty("errmsg") String errorMessage,
                            @JsonAlias("news_item") List<DraftItem> articles) implements ServerResponse {}

    @Override
    public CompletableFuture<Void> tear(String mediaId) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.THROW_DRAFT_TO_TRASH.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new MediaRequest(mediaId))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(t -> t.isSuccess() ?
                        Mono.<Void>empty():
                        Mono.error(WeixinExceptionUtil.create(t.errorCode(), t.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<Void> revise(String mediaId, int index, ManualScript draft) {
        ReviseDraftRequest body = new ReviseDraftRequest(mediaId, index,
                new ArticleItem(draft.title(), draft.thumbMediaId(), draft.displayCover() ? TRUE : FALSE, null,
                        draft.author(), draft.digest(), draft.content(), null, draft.contentSourceUrl(),
                        draft.commentEnabled() ? TRUE : FALSE, draft.onlyFansComment() ? TRUE : FALSE, null));
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.REVISE_DRAFT.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(t -> t.isSuccess() ?
                        Mono.<Void>empty():
                        Mono.error(WeixinExceptionUtil.create(t.errorCode(), t.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    record ReviseDraftRequest(@JsonProperty("media_id") String mediaId,
                              int index,
                              @JsonProperty("articles") ArticleItem article) {}

    @Override
    public CompletableFuture<Integer> count() {
        Function<String, CompletableFuture<Integer>> curl = (accessToken) -> httpClient.get().
                uri(WeixinUrl.COUNT_DRAFT.getUrl(), accessToken)
                .retrieve()
                .bodyToMono(CountDraftResponse.class)
                .flatMap(t -> t.isSuccess() ?
                        Mono.just(t.count()):
                        Mono.error(WeixinExceptionUtil.create(t.errorCode(), t.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param count 草稿的总数
     */
    record CountDraftResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg") String errorMessage,
                              @JsonAlias("total_count") Integer count) implements ServerResponse {}

    @Override
    public CompletableFuture<? extends Pageable<? extends Publication>> list(int offset, int count, boolean contentless) {
        Function<String, CompletableFuture<ListPermanentMaterialResponse>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.RETRIEVE_MANY_DRAFT.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ListDraftRequest(offset, count, contentless ? TRUE : FALSE))
                .retrieve()
                .bodyToMono(ListPermanentMaterialResponse.class)
                .flatMap(t -> t.isSuccess() ?
                        Mono.just(t):
                        Mono.error(WeixinExceptionUtil.create(t.errorCode(), t.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param offset 从全部素材的该偏移位置开始返回，0表示从第一个素材返回
     * @param count 返回素材的数量，取值在1到20之间
     * @param contentless 1 表示不返回 content 字段，0 表示正常返回，默认为 0
     */
    record ListDraftRequest(int offset, int count, @JsonProperty("no_content") Integer contentless) {}



    @Override
    public CompletableFuture<? extends PublishResult> publish(String mediaId) {
        // TODO 检查mediaId为已保存草稿
        Function<String, CompletableFuture<PublishResponse>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.PUBLISH.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new MediaRequest(mediaId))
                .retrieve()
                .bodyToMono(PublishResponse.class)
                .flatMap(t -> t.isSuccess() ?
                        Mono.just(t) :
                        Mono.error(WeixinExceptionUtil.create(t.errorCode(), t.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param publishId 发布任务的id
     * @param msgDataId 消息的数据ID
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PublishResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg") String errorMessage,
                           @JsonAlias("publish_id") String publishId,
                           @JsonAlias("msg_data_id") String msgDataId) implements ServerResponse, PublishResult {}

    @Override
    public CompletableFuture<? extends PublishStatusResult> status(String publishId) {
        // TODO 检查publishId是否为已发布草稿
        Function<String, CompletableFuture<PublishStatusResponse>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.POLL_PUBLISH_STATUS.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PublishStatusRequest(publishId))
                .retrieve()
                .bodyToMono(PublishStatusResponse.class)
                .flatMap(t -> t.isSuccess() ?
                        Mono.just(t) :
                        Mono.error(WeixinExceptionUtil.create(t.errorCode(), t.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    record PublishStatusRequest(@JsonProperty("publish_id") String publishId) {}

    /**
     * @param articleId 当发布状态为0时（即成功）时，返回图文的 article_id，可用于“客服消息”场景
     * @param status 发布状态，0:成功, 1:发布中，2:原创失败, 3: 常规失败, 4:平台审核不通过, 5:成功后用户删除所有文章, 6: 成功后系统封禁所有文章
     * @param articles 发布成功时返回文章清单
     * @param failedArticleIndexes 当发布状态为2或4时，返回不通过的文章编号，第一篇为 1；其他发布状态则为空
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PublishStatusResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg") String errorMessage,
                                 @JsonAlias("publish_id") String id,
                                 @JsonAlias("article_id") String articleId,
                                 int status,
                                 @JsonAlias("article_detail") ArticlesWrapper articles,
                                 @JsonAlias("fail_idx") int[] failedArticleIndexes) implements ServerResponse, PublishStatusResult {
        @Override
        public List<? extends ArticleStatus> successArticles() {
            return articles != null ? articles.items() : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ArticlesWrapper(int count, @JsonAlias("item") List<ArticleStatusResult> items) {}

    /**
     * @param id 当发布状态为0时（即成功）时，返回文章对应的编号
     * @param url 当发布状态为0时（即成功）时，返回图文的永久链接
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ArticleStatusResult(@JsonAlias("idx") int id, @JsonAlias("article_url") String url) implements ArticleStatus {}

    @Override
    public CompletableFuture<Void> cancel(String articleId, int index) {
        // TOOD 检查articleId是否为已发布成功
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.REMOVE_PUBLISHED.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CancelPublishedRequest(articleId, index))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(t -> t.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(t.errorCode(), t.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param articleId 成功发布时返回的 article_id
     * @param index 要删除的文章在图文消息中的位置，第一篇编号为1，该字段不填或填0会删除全部文章
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record CancelPublishedRequest(@JsonProperty("article_id") String articleId, Integer index) {}

    @Override
    public CompletableFuture<? extends List<? extends Paper>> view(String articleId) {
        // TOOD 检查articleId是否为已发布成功
        Function<String, CompletableFuture<List<ArticleItem>>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.RETRIEVE_PUBLISHED.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ViewPublishedPaperRequest(articleId))
                .retrieve()
                .bodyToMono(ViewPublishedPaperResponse.class)
                .flatMap(t -> t.isSuccess() ?
                        Mono.just(t.items()) :
                        Mono.error(WeixinExceptionUtil.create(t.errorCode(), t.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param articleId 要获取的草稿的article_id
     */
    record ViewPublishedPaperRequest(@JsonProperty("article_id") String articleId) {}

    record ViewPublishedPaperResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg") String errorMessage,
                                      @JsonAlias("news_item") List<ArticleItem> items) implements ServerResponse {}

    @Override
    public CompletableFuture<? extends Pageable<? extends Publication>> retrievePublished(int offset, int count, boolean contentless) {
        Function<String, CompletableFuture<ListPermanentMaterialResponse>> curl = (accessToken) -> httpClient.post().
                uri(WeixinUrl.GET_PUBLISHED_ARTICLES.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ListDraftRequest(offset, count, contentless ? TRUE : FALSE))
                .retrieve()
                .bodyToMono(ListPermanentMaterialResponse.class)
                .flatMap(t -> t.isSuccess() ?
                        Mono.just(t) :
                        Mono.error(WeixinExceptionUtil.create(t.errorCode(), t.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }



    @Override
    public CompletableFuture<Void> addAccount(@NotNull String account, @NotNull String nickname, String password) {
        // TODO ACCOUNT_FMT.formatted(account, ?)
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.ADD_CUSTOM_SERVICE_ACCOUNT.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CustomServiceAccountRequest(account, nickname, password))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken()
                .thenCompose(curl);
    }

    @Override
    public CompletableFuture<Void> bindAccountInviting(@NotNull String account, @NotNull String openId) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.INVITE_CUSTOM_SERVICE_BIND_ACCOUNT.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new BindInviting(account, openId))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    record BindInviting(@JsonProperty("kf_account") String account, @JsonProperty("invite_wx") String openId) {}

    @Override
    public CompletableFuture<Void> updateAccount(@NotNull String account, @NotNull String nickname, String password) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.UPDATE_CUSTOM_SERVICE_ACCOUNT.getUrl(), accessToken)
                .bodyValue(new CustomServiceAccountRequest(account, nickname, password))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<Void> removeAccount(@NotNull String account) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post().uri(WeixinUrl.DELETE_CUSTOM_SERVICE_ACCOUNT.getUrl(), accessToken)
                .bodyValue(new CustomServiceAccountRequest(account, null, null))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param account 完整客服账号，格式为：账号前缀@公众号微信号
     * @param nickname 客服昵称，最长6个汉字或12个英文字符
     * @param password 客服账号登录密码，格式为密码明文的32位加密MD5值。该密码仅用于在公众平台官网的多客服功能中使用，若不使用多客服功能，则不必设置密码
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record CustomServiceAccountRequest(String account, String nickname, String password) {}

    @Override
    public CompletableFuture<Void> changeAvatar(@NotNull InputStream is, @NotNull String filename, String account) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) ->
                multipart(httpClient.post().uri(WeixinUrl.SET_CUSTOM_SERVICE_AVATAR.getUrl(), accessToken, account), is, MEDIA_PARAM_NAME, filename)
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<List<Account>> listAccounts() {
        Function<String, CompletableFuture<List<Account>>> curl = (accessToken) -> httpClient.get()
                .uri(WeixinUrl.GET_CUSTOM_SERVICE_ACCOUNTS.getUrl(), accessToken)
                .retrieve()
                .bodyToMono(AccountListResponse.class)
                .map(t -> {
                    if (t.list() == null) {
                        log.error("request '{}' returning errcode={}, errmsg={}", WeixinUrl.GET_CUSTOM_SERVICE_ACCOUNTS.getPath(), t.errorCode(), t.errorMessage());
                        return Collections.<Account>emptyList();
                    }
                    List<Account> list = new ArrayList<>(t.list.size());
                    for (AccountItem item : t.list()) {
                        list.add(new Account(item.id(), item.account(), item.nickname(), item.avatar()));
                    }
                    return list;
                })
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccountListResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg")  String errorMessage,
                               @JsonProperty("kf_list") List<AccountItem> list) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccountItem(@JsonProperty("kf_id") String id,
                       @JsonProperty("kf_account") String account,
                       @JsonProperty("kf_nick") String nickname,
                       @JsonProperty("kf_headimgurl") String avatar) {}

    @Override
    public CompletableFuture<Void> echo(@NotNull ReplyMessage replyMessage) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.SEND_CUSTOM_SERVICE_MESSAGE.getUrl(), accessToken)
                .bodyValue(replyMessage.toJson())
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<Void> displayStatus(String toOpenId, Command command) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.DISPLAY_CUSTOM_SERVICE_TYPING_STATUS.getUrl(), accessToken)
                .bodyValue(new TypingStatus(toOpenId, command.getValue()))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record TypingStatus(@JsonProperty("touser") String toOpenId, String command) {}




    // 用户管理




    @Override
    public CompletableFuture<? extends UserLabel> labeling(String name) {
        // TODO 检查标签名是否存在，标签数量是否超过100
        Function<String, CompletableFuture<LabelInfo>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.CREATE_LABEL.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LabelWrapper(new LabelInfo(name, null ,null)))
                .retrieve()
                .bodyToMono(CreateLabelResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r.label()) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record LabelWrapper(LabelInfo tag) {}

    /**
     * @param name 标签名，UTF8编码
     * @param id 标签id，由微信分配
     * @param count
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record LabelInfo(String name, Integer id, Integer count) implements UserLabel {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CreateLabelResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg")  String errorMessage,
                               @JsonAlias(value = {"tag"}) LabelInfo label,
                               @JsonAlias(value = {"tags"}) List<LabelInfo> tags) implements ServerResponse {}

    @Override
    public CompletableFuture<? extends List<? extends UserLabel>> labeled() {
        Function<String, CompletableFuture<List<LabelInfo>>> curl = (accessToken) -> httpClient.get()
                .uri(WeixinUrl.RETRIEVE_LABEL.getUrl(), accessToken)
                .retrieve()
                .bodyToMono(CreateLabelResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r.tags()) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<Void> relabel(int tagId, String name) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.REVISE_LABEL.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LabelWrapper(new LabelInfo(name, tagId, null)))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<Void> unlabeling(int tagId) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.REMOVE_LABEL.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LabelWrapper(new LabelInfo(null, tagId, null)))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<? extends Pageable<String>> listLabeled(int tagId, String cursor) {
        Function<String, CompletableFuture<ListTaggedUserResponse>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.LIST_LABELED_USERS.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ListTaggedUserRequest(tagId, cursor))
                .retrieve()
                .bodyToMono(ListTaggedUserResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param tagId 标签id
     * @param fromOpenId 第一个拉取的OPENID，不填默认从头开始拉取
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record ListTaggedUserRequest(@JsonProperty("tagid") int tagId, @JsonProperty("next_openid") String fromOpenId) {}

    /**
     * @param all 总粉丝数（仅获取用户列表返回）
     * @param count 这次获取的粉丝数量
     * @param data 粉丝列表
     * @param cursorId 拉取列表最后一个用户的openid
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ListTaggedUserResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg")  String errorMessage,
                                  @JsonAlias("total") Integer all,
                                  @JsonAlias("count") int count,
                                  UserWrapper data,
                                  @JsonAlias("next_openid") String cursorId) implements ServerResponse, Pageable<String> {
        @Override
        public int total() {
            if (all == null) {
                throw new AbstractMethodError();
            }
            return all;
        }
        @Override
        public int offset() {
            throw new AbstractMethodError();
        }
        @Override
        public int pageSize() {
            throw new AbstractMethodError();
        }
        @Override
        public List<String> items() {
            return data != null ? data.users() : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserWrapper(@JsonAlias("openid") List<String> users) {}

    @Override
    public CompletableFuture<Void> mark(List<String> users, int tagId) {
        // TODO 检查粉丝是否属于该appId，检查粉丝标签用户数是否低于20
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.LABELING.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LabelingRequest(users, tagId))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param fans 粉丝列表，每次传入的openid列表个数不能超过50个
     * @param tagId 标签id
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record LabelingRequest(@JsonProperty("openid_list") List<String> fans, @JsonProperty("tagid") Integer tagId) {}

    @Override
    public CompletableFuture<Void> unmark(List<String> users, int tagId) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.UNLABELING.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LabelingRequest(users, tagId))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<? extends Customer> getCustomerInfo(WeixinClientUser clientUser) {
        Function<String, WebClient.RequestHeadersSpec> spec = (accessToken) ->  clientUser.labelOnly() ?
                httpClient.post().uri(WeixinUrl.LABELED.getUrl(), accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new GetCustomerRequest(clientUser.openId(), null)) :
                httpClient.get().uri(WeixinUrl.RETRIEVE_FANS_INFO.getUrl(), accessToken, clientUser.openId(), clientUser.language());
        Function<String, CompletableFuture<GetCustomerResponse>> curl = (accessToken) -> spec.apply(accessToken)
                .retrieve()
                .bodyToMono(GetCustomerResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param openId 用户的标识，对当前公众号唯一
     * @param language 用户的语言，简体中文为zh_CN
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record GetCustomerRequest(@JsonProperty("openid") String openId, @JsonProperty("lang") String language) {}

    /**
     * @param subscribeState 用户是否订阅该公众号标识，值为0时，代表此用户没有关注该公众号，拉取不到其余信息
     * @param openId 用户的标识，对当前公众号唯一
     * @param language 用户的语言，简体中文为zh_CN
     * @param subscriptionAt 用户关注时间，为时间戳。如果用户曾多次关注，则取最后关注时间
     * @param unionId 将公众号绑定到微信开放平台帐号后，才会出现该字段
     * @param remark 公众号运营者对粉丝的备注
     * @param groupId 用户所在的分组ID
     * @param tags 用户被打上的标签ID列表
     * @param subscribeScene 返回用户关注的渠道来源
     * @param scene 二维码扫码场景
     * @param sceneDescription 二维码扫码场景描述
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GetCustomerResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg")  String errorMessage,
                               @JsonAlias("subscribe") Integer subscribeState,
                               @JsonAlias("openid") String openId,
                               String language,
                               @JsonAlias("subscribe_time") Long subscriptionAt,
                               @JsonAlias("unionid") String unionId,
                               String remark,
                               @JsonAlias("groupid") String groupId,
                               @JsonAlias("tagid_list") List<Integer> tags,
                               @JsonAlias("subscribe_scene") String subscribeScene,
                               @JsonAlias("qr_scene") String scene,
                               @JsonAlias("qr_scene_str") String sceneDescription) implements ServerResponse, Customer {
        @Override
        public Boolean subscribed() {
            if (subscribeState == null) {
                return null;
            } else {
                return subscribeState == 1;
            }
        }
        @Override
        public LocalDateTime subscriptionTime() {
            return subscriptionAt != null ? LocalDateTime.ofEpochSecond(subscriptionAt * 1000, 0, ZoneOffset.UTC) : null;
        }
    }

    @Override
    public CompletableFuture<Void> remark(String openId, String comment) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.REMARK_FANS.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RemarkRequest(openId, comment))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param openId 用户标识
     * @param remark 新的备注名，长度必须小于30字节
     */
    record RemarkRequest(@JsonProperty("openid") String openId, String remark) {}

    @Override
    public CompletableFuture<? extends List<? extends Customer>> listCustomers(WeixinClientUser[] clientUsers) {
        Function<String, CompletableFuture<List<GetCustomerResponse>>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.BATCH_RETRIEVE_FANS.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new BatchRetrieveFansRequest(Arrays.stream(clientUsers)
                        .map(x -> new GetCustomerRequest(x.openId(), x.language()))
                        .collect(Collectors.toList()))
                )
                .retrieve()
                .bodyToMono(BatchRetrieveFansResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.justOrEmpty(r.fans()) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param fans 最多支持一次拉取100条
     */
    record BatchRetrieveFansRequest(@JsonProperty("user_list") List<GetCustomerRequest> fans) {}

    record BatchRetrieveFansResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg")  String errorMessage,
                                     @JsonAlias("user_info_list") List<GetCustomerResponse> fans) implements ServerResponse {}

    @Override
    public CompletableFuture<? extends Pageable<String>> listUsers(String fromOpenId) {
        Function<String, CompletableFuture<ListTaggedUserResponse>> curl = (accessToken) -> httpClient.get()
                .uri(WeixinUrl.RETRIEVE_FANS_ID.getUrl(), accessToken, fromOpenId)
                .retrieve()
                .bodyToMono(ListTaggedUserResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<Void> block(List<String> users) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.BLOCK_FANS.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LabelingRequest(users, null))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<Void> unblock(List<String> users) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.UNBLOCK_FANS.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LabelingRequest(users, null))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<? extends Pageable<String>> retrieveBlocked(String fromOpenId) {
        Function<String, CompletableFuture<ListTaggedUserResponse>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.LIST_BLOCKED_FANS.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GetBlockedRequest(fromOpenId))
                .retrieve()
                .bodyToMono(ListTaggedUserResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    record GetBlockedRequest(@JsonProperty("begin_openid") String fromOpenId) {}




    // 基础消息能力：模板消息与群发




    @Override
    public CompletableFuture<? extends List<? extends MessageTemplate>> retrieveTemplates() {
        Function<String, CompletableFuture<List<MsgTplResult>>> curl = (accessToken) -> httpClient.get()
                .uri(WeixinUrl.GET_ALL_TEMPLATES.getUrl(), accessToken)
                .retrieve()
                .bodyToMono(ListMessageTemplateResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r.items()) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ListMessageTemplateResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg")  String errorMessage,
                                       @JsonAlias("template_list") List<MsgTplResult> items) implements ServerResponse {}

    /**
     * @param id 模板ID
     * @param title 模板标题
     * @param primaryIndustry 模板所属行业的一级行业
     * @param secondaryIndustry 模板所属行业的二级行业
     * @param content 模板内容
     * @param example 模板示例
     */
    record MsgTplResult(@JsonAlias("template_id") String id,
                        String title,
                        @JsonAlias("primary_industry") String primaryIndustry,
                        @JsonAlias("deputy_industry") String secondaryIndustry,
                        String content, String example) implements MessageTemplate {}

    @Override
    public CompletableFuture<Void> removeMessageTemplate(String id) {
        // TODO 检查模板是否存在
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.DELETE_TEMPLATE.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SendTemplateMsgRequest(null, id, null, null, null, null, null, null))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty() :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<Long> send(TemplateMessage msg) {
        // TODO 检查模板是否存在，消息是否重复
        Function<String, CompletableFuture<Long>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.SEND_TEMPLATE_MESSAGE.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SendTemplateMsgRequest.from(msg))
                .retrieve()
                .bodyToMono(SendTemplateMsgResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r.msgId()) :
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param receiver 填接收消息的用户openid
     * @param templateId 消息模板ID
     * @param url 点击消息跳转的链接
     * @param programlet 跳小程序所需数据，不需跳小程序可不用传该数据
     * @param scene 订阅场景值(仅订阅推送用)
     * @param title 消息标题，15字以内(仅订阅推送用)
     * @param msgId
     * @param data 消息正文，value为消息内容文本（200字以内），没有固定格式，可用\n换行，color为整段消息内容的字体颜色
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record SendTemplateMsgRequest(@JsonProperty("touser") String receiver,
                                  @JsonProperty("template_id") String templateId, String url,
                                  @JsonProperty("miniprogram") ProgramletWrapper programlet,
                                  String scene, String title,
                                  @JsonProperty("client_msg_id") String msgId, Map<String, PlaceHolderReplacer> data) {
        public static SendTemplateMsgRequest from(TemplateMessage msg) {
            Map<String, PlaceHolderReplacer> properties = null;
            if (msg.data() != null && !msg.data().isEmpty()) {
                properties = new HashMap<>(msg.data().size());
                for (Map.Entry<String, ? extends PlaceHolder> kv : msg.data().entrySet()) {
                    properties.put(kv.getKey(), PlaceHolderReplacer.from(kv.getValue()));
                }
            }
            return new SendTemplateMsgRequest(msg.receiver(), msg.templateId(), msg.url(), ProgramletWrapper.from(msg),
                    msg.scene(), msg.title(),
                    msg.msgId(), properties);
        }
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record ProgramletWrapper(@JsonProperty("appid") String appId, @JsonProperty("pagepath") String pagePath) {
        public static ProgramletWrapper from(TemplateMessage msg) {
            return new ProgramletWrapper(msg.appId(), msg.pagePath());
        }
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record PlaceHolderReplacer(String value, String color) {
        public static PlaceHolderReplacer from(PlaceHolder placeHolder) {
            return new PlaceHolderReplacer(placeHolder.value(), placeHolder.color());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendTemplateMsgResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg")  String errorMessage,
                                   @JsonAlias("msgid") Long msgId) implements ServerResponse {}

    @Override
    public CompletableFuture<Void> pushMessage(TemplateMessage content) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.PUSH_TEMPLATE_MESSAGE_TO_SUBSCRIBERS.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SendTemplateMsgRequest.from(content))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty():
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @Override
    public CompletableFuture<? extends MessageReceipt> broadcast(String groupId, List<String> users, MessageType type, Material material, boolean forcePublish) {
        // TODO 按标签群发则检查标签是否存在，订阅号每天至多1次群发给所有人，服务号每月4次。图文消息的mediaId来源于草稿或上传图文
        MediaRequest news = null;
        TextToBroadcast text = null;
        MediaRequest voice = null;
        VideoToBroadcast video = null;
        ImagesToBroadcast images = null;
        Card card = null;
        switch (type) {
            case MP_NEWS -> news = new MediaRequest(material.id());
            case TEXT -> text = new TextToBroadcast(material.id());
            case VOICE -> voice = new MediaRequest(material.id());
            case MP_VIDEO -> {
                if (material instanceof VideoMaterial vm) {
                    video = new VideoToBroadcast(material.id(), vm.title(), vm.description());
                }
            }
            case IMAGE -> {if (material instanceof ImageMaterial im) {
                images = new ImagesToBroadcast(im.imageIds(), im.recommend(), im.enableComment() ? TRUE : FALSE, im.onlyFansComment() ? TRUE : FALSE);
            }}
            case CARD -> card = new Card(material.id(), null);
            default -> throw new IllegalArgumentException("不支持的群发类型：" + type);
        }
        BroadcastFilter filter = users != null && !users.isEmpty() ? null : new BroadcastFilter(groupId == null, groupId);
        final BroadcastRequest body = new BroadcastRequest(filter, users, news, text, images, voice, video, card, type.getValue(), forcePublish ? TRUE : FALSE, null);

        String urlTemplate = users == null || users.isEmpty() ? WeixinUrl.PUBLISH_TO_GROUP.getUrl() : WeixinUrl.PUBLISH_TO_PEERS.getUrl();

        Function<String, CompletableFuture<BroadcastResponse>> curl = (accessToken) -> httpClient.post()
                .uri(urlTemplate, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(BroadcastResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r):
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param filter 用于设定图文消息的接收者
     * @param users 填写图文消息的接收者，一串OpenID列表，OpenID最少2个，最多10000个
     * @param news 用于设定即将发送的图文消息（通过上传图文或新建草稿得到mediaId）
     * @param text 文本
     * @param images 图像（通过新增素材得到的mediaId）
     * @param voice 语音/音频（通过新增素材得到的mediaId）
     * @param video 视频通过新增素材得到的mediaId）
     * @param card
     * @param type 群发的消息类型，图文消息为mpnews，文本消息为text，语音为voice，音乐为music，图片为image，视频为video，卡券为wxcard
     * @param forcePublish 图文消息被判定为转载时，是否继续群发。 1为继续群发（转载），0为停止群发。 该参数默认为0
     * @param clientMsgId 开发者侧群发msgid，长度限制64字节，如不填，则后台默认以群发范围和群发内容的摘要值做为clientmsgid。微信后台将对 24 小时内的群发记录进行检查，如果该 clientmsgid 已经存在一条群发记录，则会拒绝本次群发请求，返回已存在的群发msgid
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record BroadcastRequest(BroadcastFilter filter,
                            @JsonProperty("touser") List<String> users,
                            @JsonProperty("mpnews") MediaRequest news,
                            TextToBroadcast text,
                            ImagesToBroadcast images,
                            MediaRequest voice,
                            @JsonProperty("mpvideo") VideoToBroadcast video,
                            @JsonProperty("wxcard") Card card,
                            @JsonProperty("msgtype") String type,
                            @JsonProperty("send_ignore_reprint") Integer forcePublish,
                            @JsonProperty("clientmsgid") String clientMsgId) {}

    /**
     * @param all 用于设定是否向全部用户发送，值为true或false
     * @param groupId 群发到的标签的tag_id
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record BroadcastFilter(@JsonProperty("is_to_all") Boolean all, @JsonProperty("tag_id") String groupId) {}

    /**
     * @param recommend 推荐语，不填则默认为“分享图片”
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record ImagesToBroadcast(@JsonProperty("media_ids") String[] ids, String recommend,
                             @JsonProperty("need_open_comment") Integer commentStats,
                             @JsonProperty("only_fans_can_comment") Integer fansCommentStats) {}

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record VideoToBroadcast(@JsonProperty("media_id") String mediaId, String title, String description) {}

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record TextToBroadcast(String content) {}

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record Card(@JsonProperty("card_id") String id, @JsonProperty("card_ext") CardExt ext) {}

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record CardExt(String code, @JsonProperty("openid") String openId, Long timestamp, String signature) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BroadcastResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg")  String errorMessage,
                             @JsonAlias("msg_id") long msgId,
                             @JsonAlias("msg_data_id") Long msgDataId) implements MessageReceipt, ServerResponse {}

    @Override
    public CompletableFuture<Void> cancelBroadcast(String msgId, Integer newsIndex, String url) {
        // TODO 检查消息是否发送成功，见擦汗消息类型是否为图文消息或视频消息
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.CANCEL_PUBLISH.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CancelBroadcastRequest(msgId, newsIndex, url))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty():
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param msgId 发送出去的消息ID
     * @param index 要删除的文章在图文消息中的位置，第一篇编号为1，该字段不填或填0会删除全部文章
     * @param url 要删除的文章url，当msg_id未指定时该参数才生效
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record CancelBroadcastRequest(@JsonProperty("msg_id") String msgId,
                                  @JsonProperty("article_idx") Integer index,
                                  String url) {}

    @Override
    public CompletableFuture<Void> preview(String openId, String name, MessageType type, Material material) {
        MediaRequest news = null;
        TextToBroadcast text = null;
        MediaRequest voice = null;
        MediaRequest video = null;
        MediaRequest image = null;
        Card card = null;
        switch (type) {
            case MP_NEWS -> news = new MediaRequest(material.id());
            case TEXT -> text = new TextToBroadcast(material.id());
            case VOICE -> voice = new MediaRequest(material.id());
            case MP_VIDEO -> video = new MediaRequest(material.id());
            case IMAGE -> image = new MediaRequest(material.id());
            case CARD -> {
                if (material instanceof CardPreviewMaterial cp) {
                    card = new Card(material.id(),
                            new CardExt(cp.code(), cp.openId(), cp.timestamp() != null ? cp.timestamp().toEpochSecond(ZoneOffset.UTC) / 1000 : null, cp.signature()));
                }
            }
            default -> throw new IllegalArgumentException("不支持的群发类型：" + type);
        }
        final BroadcastPreviewRequest body = new BroadcastPreviewRequest(openId, name, news, text, image, voice, video, card, type.getValue());

        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.PREVIEW_PUBLISH.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty():
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record BroadcastPreviewRequest(@JsonProperty("touser") String openId, @JsonProperty("towxname") String name,
                            @JsonProperty("mpnews") MediaRequest news,
                            TextToBroadcast text,
                            MediaRequest image,
                            MediaRequest voice,
                            @JsonProperty("mpvideo") MediaRequest video,
                            @JsonProperty("wxcard") Card card,
                            @JsonProperty("msgtype") String type) {}

    @Override
    public CompletableFuture<String> fetchBroadcastStatus(String msgId) {
        Function<String, CompletableFuture<String>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.GET_PUBLISH_STATUS.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CancelBroadcastRequest(msgId, null, null))
                .retrieve()
                .bodyToMono(BroadcastStatusResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r.status()):
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    /**
     * @param status 消息发送后的状态，SEND_SUCCESS表示发送成功，SENDING表示发送中，SEND_FAIL表示发送失败，DELETE表示已删除
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record BroadcastStatusResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg")  String errorMessage,
                           @JsonProperty("msg_id") String msgId, @JsonProperty("msg_status") String status) implements ServerResponse {}

    @Override
    public CompletableFuture<Void> changeBroadcastSpeed(int level) {
        Function<String, CompletableFuture<Void>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.SET_PUBLISH_SPEED.getUrl(), accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ConfigureSendSpeedRequest(level))
                .retrieve()
                .bodyToMono(DefaultResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.<Void>empty():
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    record ConfigureSendSpeedRequest(@JsonProperty("speed") int level) {}

    @Override
    public CompletableFuture<? extends Transmission> retrieveBroadcastSpeed() {
        Function<String, CompletableFuture<RetrieveBroadcastSpeedResponse>> curl = (accessToken) -> httpClient.post()
                .uri(WeixinUrl.GET_PUBLISH_SPEED.getUrl(), accessToken)
                .retrieve()
                .bodyToMono(RetrieveBroadcastSpeedResponse.class)
                .flatMap(r -> r.isSuccess() ?
                        Mono.just(r):
                        Mono.error(WeixinExceptionUtil.create(r.errorCode(), r.errorMessage())))
                .toFuture();
        return getOrRefreshAccessToken().thenCompose(curl);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RetrieveBroadcastSpeedResponse(@JsonProperty("errcode") int errorCode, @JsonProperty("errmsg")  String errorMessage,
                                          @JsonAlias("speed") int level,
                                          @JsonAlias("realspeed") int qpm) implements Transmission, ServerResponse {
        @Override
        public int qps() {
            return qpm * 10000 / 60;
        }
    }
}
