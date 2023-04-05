package weixin.mp.infrastructure.endpoint;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;
import weixin.mp.domain.Context;
import weixin.mp.facade.WeixinArticleApiFacade;
import weixin.mp.facade.dto.Publication;
import weixin.mp.infrastructure.cache.CacheName;
import weixin.mp.infrastructure.config.ManagementProperties;
import weixin.mp.infrastructure.endpoint.vo.Draft;
import weixin.mp.infrastructure.endpoint.vo.DraftCollection;
import weixin.mp.infrastructure.endpoint.vo.Pagination;
import weixin.mp.infrastructure.endpoint.vo.Press;
import weixin.mp.infrastructure.endpoint.vo.RevisedDraft;
import weixin.mp.infrastructure.rpc.Weixin;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

@Controller
public class WeixinMediaController extends Tenant {

    @Autowired
    private ManagementProperties managementProperties;

    @Autowired
    @Qualifier("xlock")
    private Lock xlock;

    @PostMapping(ExposedPath.PAPER_DRAFT)
    @ResponseBody
    public Mono<String> create(@PathVariable("id") String id, @Validated @RequestBody DraftCollection draft) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinArticleApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.draft(draft.articles()));
    }

    @GetMapping(ExposedPath.PAPER_DRAFT)
    @ResponseBody
    public Mono<Integer> count(@PathVariable("id") String id) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinArticleApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.count());
    }

    @GetMapping(value = ExposedPath.PAPER_DRAFT, params = {"offset", "count"})
    @ResponseBody
    public Mono<Pagination<? extends Publication>> list(@PathVariable("id") String id,
                                                        @RequestParam("offset") @Min(0) int offset,
                                                        @RequestParam("count") @Min(1) @Max(20) int count,
                                                        @RequestParam(value = "contentless", defaultValue = "false", required = false) boolean contentless) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinArticleApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.list(offset, count, contentless))
                .map(x -> new Pagination(x.total(), x.offset(), x.pageSize(),
                        x.items().stream().map(RevisedDraft::from).collect(Collectors.toList())));
    }

    @GetMapping(ExposedPath.ONE_PAPER_DRAFT)
    @ResponseBody
    public Mono<List<Press>> index(@PathVariable("id") String id, @PathVariable("mediaId") String mediaId) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinArticleApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.retrieve(mediaId))
                .map(x -> x.stream().map(Press::from).collect(Collectors.toList()));
    }

    @DeleteMapping(ExposedPath.ONE_PAPER_DRAFT)
    @ResponseBody
    public Mono<Void> remove(@PathVariable("id") String id, @PathVariable("mediaId") String mediaId) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinArticleApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.tear(mediaId));
    }

    @PutMapping(ExposedPath.ONE_PAPER_DRAFT)
    @ResponseBody
    public Mono<Void> revise(@PathVariable("id") String id, @PathVariable("mediaId") String mediaId,
                             @RequestParam(value = "index", defaultValue = "0", required = false) int index,
                             @Validated @RequestBody Draft draft) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinArticleApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.revise(mediaId, index, draft));
    }

    @PostMapping(ExposedPath.PAPER)
    @ResponseBody
    public Mono<PublishResponse> publish(@PathVariable("id") String id, @RequestParam("mediaId") String mediaId) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinArticleApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.publish(mediaId)).map(x -> new PublishResponse(x.publishId(), x.msgDataId()));
    }

    @GetMapping(value = ExposedPath.PAPER, params = {"publishId"})
    @ResponseBody
    public Mono<PublishStatusResponse> pollPublishStatus(@PathVariable("id") String id, @RequestParam("publishId") String publishId) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinArticleApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.status(publishId))
                .map(x -> new PublishStatusResponse(x.status(), x.articleId(), x.successArticles(), x.failedArticleIndexes()));
    }

    @GetMapping(value = ExposedPath.PAPER, params = {"offset", "count"})
    @ResponseBody
    public Mono<Pagination<RevisedDraft>> listPublished(@PathVariable("id") String id,
                                            @RequestParam("offset") int offset, @RequestParam("count") int count,
                                            @RequestParam(value = "contentless", defaultValue = "false", required = false) boolean contentless) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinArticleApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.retrievePublished(offset, count, contentless))
                .map(x -> new Pagination(x.total(), x.offset(), x.pageSize(),
                        x.items().stream().map(RevisedDraft::from).collect(Collectors.toList())));
    }

    @GetMapping(ExposedPath.ONE_PAPER)
    @ResponseBody
    public Mono<List<Press>> viewPublished(@PathVariable("id") String id, @PathVariable("articleId") String articleId) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinArticleApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.view(articleId))
                .map(x -> x.stream().map(Press::from).collect(Collectors.toList()));
    }

    @DeleteMapping(ExposedPath.ONE_PAPER)
    @ResponseBody
    public Mono<Void> deletePublished(@PathVariable("id") String id, @PathVariable("articleId") String articleId,
                       @RequestParam(value = "index", defaultValue = "0", required = false) int index) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinArticleApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), xlock);
        return Mono.fromFuture(facade.cancel(articleId, index));
    }

    record PublishResponse(String publishId, String msgDataId) {}

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record PublishStatusResponse(int status, String articleId,
                                 List<? extends WeixinArticleApiFacade.ArticleStatus> successArticles,
                                 int[] failedArticleIndexes)  implements WeixinArticleApiFacade.PublishStatusResult {}

}
