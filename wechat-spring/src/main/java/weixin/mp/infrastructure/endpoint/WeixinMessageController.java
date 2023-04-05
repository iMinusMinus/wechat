package weixin.mp.infrastructure.endpoint;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;
import weixin.mp.domain.Context;
import weixin.mp.domain.MessageType;
import weixin.mp.facade.WeixinMessageApiFacade;
import weixin.mp.infrastructure.cache.CacheName;
import weixin.mp.infrastructure.config.ManagementProperties;
import weixin.mp.infrastructure.rpc.Weixin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

@Controller
public class WeixinMessageController extends Tenant {

    @Autowired
    private ManagementProperties managementProperties;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private Lock lock;

    @GetMapping(ExposedPath.CHAT)
    @ResponseBody
    public Mono<List<? extends WeixinMessageApiFacade.MessageTemplate>> listMessageTemplates(@PathVariable("id") String id) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMessageApiFacade weixinMessageApiFacade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(weixinMessageApiFacade.retrieveTemplates());
    }

    @PostMapping(ExposedPath.CHAT)
    @ResponseBody
    public Mono<Long> sendTplMsg(@PathVariable("id") String id, @RequestBody TplMsg msg) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMessageApiFacade weixinMessageApiFacade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(weixinMessageApiFacade.send(msg));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TplMsg(@JsonAlias("touser") @NotEmpty String receiver,
                  @JsonAlias("template_id") @NotEmpty String templateId,
                  @JsonAlias("client_msg_id") String msgId,
                  String url,
                  String appId, String pagePath,
                  @NotNull Map<String, Replacer> data) implements WeixinMessageApiFacade.TemplateMessage {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Replacer(@NotEmpty String value, String color) implements WeixinMessageApiFacade.PlaceHolder {}

    @DeleteMapping(ExposedPath.CHAT)
    @ResponseBody
    public Mono<Void> removeMessageTemplate(@PathVariable("id") String id, @RequestParam("templateId") String templateId) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMessageApiFacade weixinMessageApiFacade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(weixinMessageApiFacade.removeMessageTemplate(templateId));
    }

    @PostMapping(ExposedPath.BROADCAST)
    @ResponseBody
    public Mono<BroadcastEcho> broadcast(@PathVariable("id") String id, @RequestBody @Validated BroadcastMessage message) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMessageApiFacade weixinMessageApiFacade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(weixinMessageApiFacade.broadcast(message.groupId(), message.users(),
                        MessageType.getInstance(message.messageType()), message.material(), message.forcePublish()))
                .map(x -> new BroadcastEcho(x.msgId(), x.msgDataId()));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BroadcastMessage(String groupId,
                            @Size(min = 2, max = 10000) List<String> users,
                            @NotNull String messageType,
                            ReceivableMaterial material,
                            boolean forcePublish) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReceivableMaterial(String id,
                              String[] imageIds, String recommend, Integer commentState,
                              String title, String description) implements WeixinMessageApiFacade.ImageMaterial, WeixinMessageApiFacade.VideoMaterial {
        @Override
        public boolean enableComment() {
            return commentState != null && commentState != 0;
        }
        @Override
        public boolean onlyFansComment() {
            return commentState != null && commentState.intValue() < 0;
        }
    }

    record BroadcastEcho(long msgId, long msgDataId) {}

}
