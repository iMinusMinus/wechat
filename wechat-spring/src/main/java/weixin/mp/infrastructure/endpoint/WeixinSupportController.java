package weixin.mp.infrastructure.endpoint;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;
import weixin.mp.domain.Context;
import weixin.mp.facade.WeixinApiFacade;
import weixin.mp.infrastructure.cache.CacheName;
import weixin.mp.infrastructure.config.ManagementProperties;
import weixin.mp.infrastructure.rpc.Weixin;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.locks.Lock;

@Controller
public class WeixinSupportController extends Tenant {

    @Autowired
    private ManagementProperties managementProperties;

    @Autowired
    @Qualifier("xlock")
    private Lock xlock;

    @GetMapping(value = ExposedPath.QUICK_RESPONSE_CODE, params = {"scene"})
    @ResponseBody
    public Mono<QRCodeResult> generateQRCode(@PathVariable("id") String id,
                                                                  @Max(2592000) @RequestParam(value = "ttl", required = false) Integer ttl,
                                                                  /* @Size(min = 1, max = 64) */ @RequestParam(value = "scene", required = false) String scene) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        Integer sceneId = null;
        String sceneStr = null;
        try {
            sceneId = Integer.parseInt(scene);
        } catch (NumberFormatException ignore) {
            sceneStr = scene;
        }
        if (sceneId != null) {
            if (ttl == null && (sceneId < 0 || sceneId > 100000)) {
                return Mono.error(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "永久二维码场景值需在1~100000之间"));
            }
        }
        return Mono.fromFuture(facade.generateQuickResponseCode(ttl, sceneId, sceneStr))
                .map(x -> new QRCodeResult(x.ticket(), x.ttl(), x.url()));
    }

    record QRCodeResult(String ticket, Integer ttl, String url) implements WeixinApiFacade.GeneratedQuickResponseResult {}

    @GetMapping(value = ExposedPath.QUICK_RESPONSE_CODE, params = {"ticket"})
    @ResponseBody
    public Mono<HttpEntity<InputStreamResource>> downloadQRImage(@PathVariable("id") String id, @RequestParam("ticket") String ticket) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        mvm.add(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE);
        return Mono.fromFuture(facade.downloadQuickResponseCode(URLEncoder.encode(ticket, StandardCharsets.UTF_8)))
                .map(x -> new HttpEntity(new InputStreamResource(x), HttpHeaders.readOnlyHttpHeaders(mvm)));
    }

    @PostMapping(ExposedPath.SHORTEN)
    @ResponseBody
    public Mono<String> shorten(@PathVariable("id") String id, @RequestBody RawData raw) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.shorten(raw.data(), raw.ttl()));
    }

    record RawData(@Size(max = 4096) String data, @Max(2592000) Integer ttl) {}

    @GetMapping(ExposedPath.SHORTEN)
    @ResponseBody
    public Mono<RecoveredData> restore(@PathVariable("id") String id, @RequestParam("key") String key) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.restore(key))
                .map(r -> new RecoveredData(r.originalData(), r.createdAt(), r.ttl()));
    }

    record RecoveredData(String data, LocalDateTime createdAt, int ttl) {}
}
