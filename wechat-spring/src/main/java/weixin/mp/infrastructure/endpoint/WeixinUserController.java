package weixin.mp.infrastructure.endpoint;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
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
import weixin.mp.domain.RequestMessage;
import weixin.mp.facade.WeixinUserApiFacade;
import weixin.mp.infrastructure.cache.CacheName;
import weixin.mp.infrastructure.config.ManagementProperties;
import weixin.mp.infrastructure.endpoint.vo.Pagination;
import weixin.mp.infrastructure.rpc.Weixin;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

@Controller
public class WeixinUserController extends Tenant {

    @Autowired
    private ManagementProperties managementProperties;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private Lock lock;

    @PostMapping(ExposedPath.LABEL)
    @ResponseBody
    public Mono<UserTag> createLabel(@PathVariable("id") String id, @RequestBody @Validated(value = {Default.class}) UserTag body) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinUserApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(facade.labeling(body.name)).map(x -> new UserTag(x.id(), x.name(), x.count()));
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserTag(@NotNull(groups = {RequestMessage.class}) Integer id,
                   @NotEmpty @Size(max = 30) String name, Integer count) {}

    @GetMapping(ExposedPath.LABEL)
    @ResponseBody
    public Mono<List<UserTag>> listLabel(@PathVariable("id") String id) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinUserApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(facade.labeled())
                .map(x -> x.stream().map(i -> new UserTag(i.id(), i.name(), i.count())).collect(Collectors.toList()));
    }

    @PutMapping(ExposedPath.LABEL)
    @ResponseBody
    public Mono<Void> reviseLabel(@PathVariable("id") String id,
                                  @RequestBody @Validated(value = {RequestMessage.class, Default.class}) UserTag body) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinUserApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(facade.relabel(body.id(), body.name()));
    }

    @DeleteMapping(ExposedPath.ONE_LABEL)
    @ResponseBody
    public Mono<Void> removeLabel(@PathVariable("id") String id,
                                  @PathVariable("labelId") int labelId) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinUserApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(facade.unlabeling(labelId));
    }

    @PostMapping(ExposedPath.ONE_LABEL)
    @ResponseBody
    public Mono<Void> mark(@PathVariable("id") String id,
                           @PathVariable("labelId") int labelId,
                           @RequestBody @Size(max = 50) List<String> users) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinUserApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(facade.mark(users, labelId));
    }

    @PutMapping(ExposedPath.ONE_LABEL)
    @ResponseBody
    public Mono<Void> unmark(@PathVariable("id") String id,
                             @PathVariable("labelId") int labelId,
                             @RequestBody @Size(max = 50) List<String> users) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinUserApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(facade.unmark(users, labelId));
    }

    @GetMapping(value = ExposedPath.ONE_USER)
    @ResponseBody
    public Mono<GetUserResponse> retrieveFansInformation(@PathVariable("id") String id,
                                                 @PathVariable("userId") String userId,
                                                 @RequestParam(value = "labelOnly", required = false, defaultValue = "false") boolean labelOnly) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinUserApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(facade.getCustomerInfo(new GetUserRequest(userId, null, labelOnly)))
                .map(GetUserResponse::from);
    }

    @GetMapping(value = ExposedPath.USER, params = {"user"})
    @ResponseBody
    public Mono<List<GetUserResponse>> retrieveManyFansInformation(@PathVariable("id") String id,
                                                         @RequestParam("user") @Size(max = 100) String[] users,
                                                         @RequestParam(value = "language", required = false, defaultValue = "zh_CN") String language) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinUserApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(facade.listCustomers(Arrays.stream(users)
                        .map(s -> new GetUserRequest(s, language, false)).toArray(GetUserRequest[]::new))
                )
                .map(r -> r.stream().map(GetUserResponse::from).collect(Collectors.toList()));
    }

    @GetMapping(value = ExposedPath.USER, params = {"blocked"})
    @ResponseBody
    public Mono<Pagination<String>> retrieveManyFansId(@PathVariable("id") String id,
                                                       @RequestParam("blocked") boolean blocked,
                                                       @RequestParam(value = "cursor", required = false, defaultValue = "") String cursor) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinUserApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return blocked ?
                Mono.fromFuture(facade.retrieveBlocked(cursor))
                        .map(x -> new Pagination(x.total(), -1, 1000, x.items())) :
                Mono.fromFuture(facade.listUsers(cursor))
                        .map(x -> new Pagination(x.total(), -1, 10000, x.items()));
    }

    record GetUserRequest(@JsonAlias("openid") String openId,
                          @JsonAlias("lang") String language,
                          boolean labelOnly) implements WeixinUserApiFacade.WeixinClientUser {}

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record GetUserResponse(Boolean subscribed, String openId, String language, LocalDateTime subscriptionAt,
                           String unionId, String remark, String groupId,
                           List<Integer> labels,
                           String subscribeScene, String scene, String sceneDescription) {
        public static GetUserResponse from(WeixinUserApiFacade.Customer i) {
            return new GetUserResponse(i.subscribed(), i.openId(), i.language(), i.subscriptionTime(),
                    i.unionId(), i.remark(), i.groupId(), i.tags(), i.subscribeScene(), i.scene(), i.sceneDescription());
        }
    }

    @PutMapping(value = ExposedPath.ONE_USER, params = {"remark"})
    @ResponseBody
    public Mono<Void> updateFansInformation(@PathVariable("id") String id,
                                                         @PathVariable("userId") String userId,
                                                         @RequestParam(value = "remark") @Size(max = 30) String remark) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinUserApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(facade.remark(userId, remark));
    }

    @PutMapping(value = ExposedPath.USER, params = {"block"})
    @ResponseBody
    public Mono<Void> blockUser(@PathVariable("id") String id, @RequestParam("block") String[] users) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinUserApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(facade.block(Arrays.asList(users)));
    }

    @PutMapping(value = ExposedPath.USER, params = {"unblock"})
    @ResponseBody
    public Mono<Void> unblockUser(@PathVariable("id") String id, @RequestParam("unblock") String[] users) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinUserApiFacade facade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(facade.unblock(Arrays.asList(users)));
    }
}
