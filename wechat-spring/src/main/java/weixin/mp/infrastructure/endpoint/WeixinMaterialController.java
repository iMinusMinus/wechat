package weixin.mp.infrastructure.endpoint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.groups.Default;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;
import weixin.mp.domain.Context;
import weixin.mp.domain.MaterialType;
import weixin.mp.facade.WeixinMaterialApiFacade;
import weixin.mp.facade.dto.ManualScript;
import weixin.mp.facade.dto.Material;
import weixin.mp.facade.dto.MultiMedia;
import weixin.mp.facade.dto.Paper;
import weixin.mp.facade.dto.Publication;
import weixin.mp.infrastructure.cache.CacheName;
import weixin.mp.infrastructure.config.ManagementProperties;
import weixin.mp.infrastructure.endpoint.vo.DraftCollection;
import weixin.mp.infrastructure.endpoint.vo.Pagination;
import weixin.mp.infrastructure.endpoint.vo.Press;
import weixin.mp.infrastructure.endpoint.vo.RevisedDraft;
import weixin.mp.infrastructure.rpc.Weixin;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

@Controller
public class WeixinMaterialController extends Tenant {

    @Autowired
    private ManagementProperties managementProperties;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    @Qualifier("xlock")
    private Lock xlock;

    @PostMapping(value = ExposedPath.ASSETS, params = {"permanent"})
    @ResponseBody
    public Mono<MaterialResultVO> uploadAssets(@PathVariable("id") String id,
                                               @RequestParam(value = "permanent", defaultValue = "false") boolean permanent,
                                               @RequestParam(value = "broadcast", defaultValue = "false") boolean broadcast,
                                               @RequestPart("file") FilePart part,
                                               @RequestPart(value = "description", required = false) Description description) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMaterialApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        if (broadcast) {
            return part.content()
                    .flatMap(buffer -> Mono.fromFuture(facade.uploadImage(buffer.asInputStream(true), part.filename())))
                    .map(x -> new MaterialResultVO(null, MaterialType.IMAGE, null, x))
                    .next();
        } else {
            final WeixinMaterialApiFacade.VideoDescription videoDescription = description != null ?
                    new WeixinMaterialApiFacade.VideoDescription(description.title(), description.introduction()) : null;
            return part.content()
                    .flatMap(buffer ->
                            Mono.fromFuture(facade.upload(buffer.asInputStream(true), part.filename(), permanent, videoDescription)))
                    .map(MaterialResultVO::from)
                    .next();
        }
    }

    @PostMapping(value = ExposedPath.ASSETS)
    @ResponseBody
    public Mono<MaterialResultVO> addNews(@PathVariable("id") String id,
                                          @RequestParam(value = "broadcast", defaultValue = "true") boolean broadcast,
                                       @Validated(value = {ManualScript.class, Default.class}) @RequestBody DraftCollection drafts) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMaterialApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.addNews(drafts.articles(), broadcast))
                    .map(MaterialResultVO::from);
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record MaterialResultVO(String id, MaterialType type, LocalDateTime createdAt, String url) implements WeixinMaterialApiFacade.PermanentMaterial, WeixinMaterialApiFacade.TemporalMaterial {
        public static MaterialResultVO from(Material contract) {
            if (contract instanceof WeixinMaterialApiFacade.PermanentMaterial pm) {
                return new MaterialResultVO(pm.id(), null, null, pm.url());
            } else if (contract instanceof WeixinMaterialApiFacade.TemporalMaterial tm){
                return new MaterialResultVO(tm.id(), tm.type(), tm.createdAt(), null);
            } else if (contract instanceof WeixinMaterialApiFacade.NewsResult newsResult){
                return new MaterialResultVO(contract.id(), newsResult.type(), newsResult.createdAt(), null);
            } else {
                return new MaterialResultVO(contract.id(), null, null, null);
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Description(String title, String introduction) {}

    @GetMapping(ExposedPath.MEDIA_ASSET)
    @ResponseBody
    public Mono<HttpEntity> downloadAsset(@PathVariable("id") String id, @PathVariable("mediaId") String mediaId,
                              @RequestParam(value = "permanent", defaultValue = "false") boolean permanent) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMaterialApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.download(mediaId, permanent))
                .map(d -> d instanceof WeixinMaterialApiFacade.Stream ?
                        outputStream(((WeixinMaterialApiFacade.Stream) d).body(), ((WeixinMaterialApiFacade.Stream) d).filename()) :
                        jsonContent(d));
    }

    private HttpEntity outputStream(InputStream body, String filename) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return new HttpEntity(new InputStreamResource(body), httpHeaders); // org.springframework.http.converter.HttpMessageNotWritableException: No Encoder for [java.io.ByteArrayInputStream] with preset Content-Type 'application/octet-stream'
    }

    private HttpEntity jsonContent(Object body) {
        String title = null;
        String description = null;
        String url = null;
        List<Press> items = null;
        if (body instanceof WeixinMaterialApiFacade.News news && news.items() != null) {
            items = new ArrayList<>();
            for (Paper paper : news.items()) {
                // 未返回评论设置
                items.add(Press.from(paper));
            }
        }
        if (body instanceof WeixinMaterialApiFacade.Video){
            title = ((WeixinMaterialApiFacade.Video) body).title();
            description = ((WeixinMaterialApiFacade.Video) body).description();
            url = ((WeixinMaterialApiFacade.Video) body).url();
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity(new HyperMedia(title, description, url, items), httpHeaders);
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record HyperMedia(String title, String description, String url,
                                        List<Press> items) implements WeixinMaterialApiFacade.News, WeixinMaterialApiFacade.Video {}

    @DeleteMapping(ExposedPath.MEDIA_ASSET)
    @ResponseBody
    public Mono<Void> deletePermanentAsset(@PathVariable("id") String id, @PathVariable("mediaId") String mediaId) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMaterialApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.recycle(mediaId));
    }

    @GetMapping(value =ExposedPath.ASSETS, params = {})
    @ResponseBody
    public Mono<MaterialSummary> groupCountPermanentMaterial(@PathVariable("id") String id) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMaterialApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.countMaterialByType())
                .map(x -> new MaterialSummary(x.image(), x.news(), x.voice(), x.video()));
    }

    record MaterialSummary(int image, int news, int voice, int video) {}

    @GetMapping(value = ExposedPath.ASSETS, params = {"type"})
    @ResponseBody
    public Mono<Pagination<? extends Material>> list(@PathVariable("id") String id,
                               @RequestParam("type") MaterialType materialType,
                               @RequestParam(value = "offset", defaultValue = "0", required = false) int offset,
                               @RequestParam(value = "limit", defaultValue = "10", required = false)  @Max(20) @Min(1) int limit) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMaterialApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.list(materialType, offset, limit))
                .map(r -> new Pagination(r.total(), offset, limit, r.items().stream().map(WeixinMaterialController::fromContract).collect(Collectors.toList())));
    }

    private static Material fromContract(Material material) {
        if (material instanceof Publication print) {
            return RevisedDraft.from(print);
        } else if (material instanceof MultiMedia multiMedia) {
            return new PermanentMaterialItem(multiMedia.id(), multiMedia.name(), multiMedia.updatedAt(), multiMedia.url());
        }
        return null;
    }

    record PermanentMaterialItem(String id, String name, LocalDateTime updatedAt, String url) implements MultiMedia {}

}
