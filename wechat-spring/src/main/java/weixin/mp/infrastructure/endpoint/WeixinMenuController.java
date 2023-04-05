package weixin.mp.infrastructure.endpoint;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import weixin.mp.domain.MenuItem;
import weixin.mp.domain.MenuType;
import weixin.mp.facade.WeixinMenuApiFacade;
import weixin.mp.facade.dto.Paper;
import weixin.mp.infrastructure.cache.CacheName;
import weixin.mp.infrastructure.config.ManagementProperties;
import weixin.mp.infrastructure.rpc.Weixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

@Controller
public class WeixinMenuController extends Tenant {

    private static final Logger log = LoggerFactory.getLogger(WeixinMenuController.class);

    @Autowired
    private ManagementProperties managementProperties;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private Lock lock;

    @PostMapping(ExposedPath.MENU)
    @ResponseBody
    public Mono<String> createMenu(@PathVariable("id") String id,
                                   @Validated(value = {Button.class, Default.class}) @RequestBody Menu menu) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMenuApiFacade weixinMenuApiFacade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        List<MenuItem> items = new ArrayList<>(menu.buttons());
        return Mono.fromFuture(weixinMenuApiFacade.create(items, menu.rule));
    }

    @GetMapping(ExposedPath.MENU)
    @ResponseBody
    public Mono<WeixinMenuController.MenuBar> find(@PathVariable("id") String id,
                                                  @RequestParam(value = "custom", defaultValue = "false") boolean custom) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMenuApiFacade weixinMenuApiFacade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(weixinMenuApiFacade.get(custom))
                .map(m -> new MenuBar(Optional.ofNullable(m.fixedMenu()).map(intf -> new FixedMenu(intf.items(), intf.menuId(), intf.isEnabled())).orElse(null),
                        Optional.ofNullable(m.conditionalMenus()).map(list -> list.stream().map(intf -> new CustomMenu(intf.items(), intf.rule(), intf.menuId())).collect(Collectors.toList())).orElse(null)));
    }

    @PostMapping(value = ExposedPath.MENU, params = {"userId"})
    @ResponseBody
    public Mono<List<? extends MenuItem>> test(@PathVariable("id") String id, @RequestParam("userId") String userId) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMenuApiFacade weixinMenuApiFacade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(weixinMenuApiFacade.test(userId))
                .map(list -> list.stream().map(this::fromDto2Vo).collect(Collectors.toList()));
    }

    private Button fromDto2Vo(MenuItem item) {
        switch (item.kind()) {
            case BOX:
                assert item instanceof MenuItem.BoxItem;
                return new FoldedButton(item.name(), item.kind().getValue(), ((MenuItem.BoxItem) item).items().stream().map(this::fromDto2Vo).collect(Collectors.toList()));
            case VIEW:
                assert item instanceof MenuItem.ViewItem;
                return new ViewButton(item.name(), item.kind().getValue(), ((MenuItem.ViewItem) item).url());
            case PROGRAMLET:
                assert item instanceof MenuItem.ProgramletItem;
                MenuItem.ProgramletItem programlet = (MenuItem.ProgramletItem) item;
                return new ProgramletButton(item.name(), item.kind().getValue(),
                        programlet.url(), programlet.appId(),programlet.pagePath());
            case CLICK:
            case SCAN_QUICK_RESPONSE_CODE_WAITING:
            case SCAN_QUICK_RESPONSE_CODE_PUSH:
            case PHOTO:
            case ALBUM:
            case IMAGE_SELECT:
            case LOCATE:
                assert item instanceof MenuItem.KeyItem;
                return new KeyButton(item.name(), item.kind().getValue(), ((MenuItem.KeyItem) item).key());
            case NEWS:
                assert item instanceof MenuItem.PaperItem;
                assert item instanceof MenuItem.ValueItem;
                MenuItem.PaperItem news = (MenuItem.PaperItem) item;
                List<NewsItem> list = news.items().stream().map(x -> new NewsItem(x.title(), x.author(), x.digest(), x.content(), x.thumbMediaId(),
                        x.contentSourceUrl(), x.displayCover(), x.coverUrl(), x.url(),
                        x.commentEnabled(), x.onlyFansComment())).collect(Collectors.toList());
                return new PaperButton(item.name(), item.kind().getValue(), ((MenuItem.ValueItem) item).value(), list);
            case IMAGE:
            case VOICE:
            case VIDEO:
                // merge to value
            case NEWS1:
            case NEWS2:
            case NEWS3:
                assert item instanceof MenuItem.ValueItem;
                return new ValueButton(item.name(), item.kind().getValue(), ((MenuItem.ValueItem) item).value());
            default:
                log.warn("unknown menu type: {}", item);
                return null;
        }
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record MenuBar(FixedMenu fixedMenu, List<? extends CustomMenu> conditionalMenus) {}

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record FixedMenu(List<? extends MenuItem> items,
                     Long menuId,
                     boolean isEnabled) implements WeixinMenuApiFacade.MenuResult {
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record CustomMenu(List<? extends MenuItem> items,
                      WeixinMenuApiFacade.MenuMatchRule rule,
                      Long menuId) implements WeixinMenuApiFacade.CustomMenuResult {
    }

    @DeleteMapping(ExposedPath.MENU)
    @ResponseBody
    public Mono<Void> resetMenu(@PathVariable("id") String id,
                                @RequestParam(value = "menuId", required = false) Long menuId) {
        Context ctx = discriminate(id, managementProperties.accounts());
        WeixinMenuApiFacade weixinMenuApiFacade = new Weixin(ctx, cacheManager.getCache(CacheName.ACCESS_TOKEN), lock);
        return Mono.fromFuture(weixinMenuApiFacade.delete(menuId));
    }

    // 为避免将wechat.ddd exports/opens 给com.fasterxml.jackson.databind，重新定义传输对象

    record Menu(@NotNull @Size(min = 1, max = 3) @JsonAlias(value = {"button", "buttons"}) List<Button> buttons,
                        MatchRule rule) {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type",
            visible = true, defaultImpl = FoldedButton.class) // sub_button类型没有type字段
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = FoldedButton.class, name = "sub_button"),
            @JsonSubTypes.Type(value = KeyButton.class, names = {
                    "click", "scancode_push", "scancode_waitmsg",
                    "pic_sysphoto", "pic_weixin", "pic_photo_or_album",
                    "location_select"}
            ),
            @JsonSubTypes.Type(value = Media.class, name = "media_id"),
            @JsonSubTypes.Type(value = Article.class, name = "article_id"),
            @JsonSubTypes.Type(value = ArticleView.class, name = "article_view_limited"),
            @JsonSubTypes.Type(value = ViewButton.class, name = "view"),
            @JsonSubTypes.Type(value = ProgramletButton.class, name = "miniprogram"),
        })
    interface Button extends MenuItem {
        String type();

        @Override
        default MenuType kind() {
            return MenuType.getInstance(type());
        }
    }

    record FoldedButton(@NotEmpty
                            @Size(max = 16, groups = {Default.class, Button.class})  String name,
                            String type,
                            @Size(min = 1, max = 5) @JsonAlias(value = {"sub_button", "buttons"}) List<Button> buttons) implements Button, MenuItem.BoxItem {
        @Override
        public MenuType kind() {
            return MenuType.BOX;
        }

        @Override
        public List<? extends MenuItem> items() {
            return buttons;
        }
    }

    /**
     * click、scancode_push、scancode_waitmsg、pic_sysphoto、pic_photo_or_album、 pic_weixin、location_select：保存值到key
     */
    record KeyButton(@NotEmpty
                       @Size(max = 16, groups = {Default.class, Button.class})
                       @Size(max = 60, groups = {FoldedButton.class}) String name,
                       String type,
                       @NotEmpty @Size(max = 128) String key) implements Button, MenuItem.KeyItem {}

    /**
     * view：保存链接到url
     */
    @JsonTypeName("view")
    record ViewButton(@NotEmpty
                       @Size(max = 16, groups = {Default.class, Button.class})
                       @Size(max = 60, groups = {FoldedButton.class}) String name,
                       String type,
                       @NotEmpty @Size(max = 1024) String url) implements Button, MenuItem.ViewItem {
    }

    /**
     * 小程序菜单
     * @param name 菜单标题
     * @param url 网页链接
     * @param appId 小程序的appid
     * @param pagePath 小程序的页面路径
     */
    @JsonTypeName("miniprogram")
    record ProgramletButton(@NotEmpty
                              @Size(max = 16, groups = {Default.class, Button.class})
                              @Size(max = 60, groups = {FoldedButton.class}) String name,
                              String type,
                              @NotEmpty @Size(max = 1024) String url,
                              @NotEmpty @JsonAlias(value = {"appid", "appId"}) String appId,
                              @NotEmpty @JsonAlias(value = {"pagepath", "pagePath"}) String pagePath) implements Button, MenuItem.ProgramletItem {
    }

    record Media(@NotEmpty
                 @Size(max = 16, groups = {Default.class, Button.class})
                 @Size(max = 60, groups = {FoldedButton.class}) String name,
                 String type,
                 @NotEmpty @JsonAlias(value = {"mediaId", "media_id"}) String mediaId) implements Button, MenuItem.ValueItem {
        @Override
        public String value() {
            return mediaId;
        }
    }

    record Article(@NotEmpty
                   @Size(max = 16, groups = {Default.class, Button.class})
                   @Size(max = 60, groups = {FoldedButton.class}) String name,
                   String type,
                   @NotEmpty @JsonAlias(value = {"article_id", "articleId"}) String articleId) implements Button, MenuItem.ValueItem {
        @Override
        public String value() {
            return articleId;
        }
    }

    record ArticleView(@NotEmpty
                     @Size(max = 16, groups = {Default.class, Button.class})
                     @Size(max = 60, groups = {FoldedButton.class}) String name,
                     String type,
                     @NotEmpty @JsonAlias(value = {"article_id", "articleId"}) String articleId) implements Button, MenuItem.ValueItem {
        @Override
        public String value() {
            return articleId;
        }
    }

    record ValueButton(String name, String type, String value) implements Button, MenuItem.ValueItem {}

    record PaperButton(String name, String type, String value, List<NewsItem> items) implements Button, MenuItem.ValueItem, MenuItem.PaperItem {}

    record NewsItem(String title, String author, String digest, String content, String thumbMediaId,
                    String contentSourceUrl, boolean displayCover, String coverUrl, String url,
                    boolean commentEnabled, boolean onlyFansComment) implements Paper {}


    record MatchRule(String tagId, Platform platform) implements WeixinMenuApiFacade.MenuMatchRule {
        @Override
        public String clientPlatform() {
            return platform != null ? platform.getValue() : null;
        }
    }

    enum Platform {
        ANDROID("1"),
        IOS("2"),
        OTHER("3"),
        ;
        private final String value;

        public String getValue() {
            return value;
        }

        Platform(String value) {
            this.value = value;
        }
    }

}
