package weixin.mp.infrastructure.endpoint;

public final class ExposedPath {

    // 微信回调接口
    static final String MESSAGE = "/{id}";

    static final String JS_API = MESSAGE + "/MP_verify_{nonce}.txt";


    // 代理请求微信二维码、短key
    static final String QUICK_RESPONSE_CODE = "/{id}/qr";
    static final String SHORTEN = "/{id}/shorten";

    // 代理请求微信菜单接口
    static final String MENU = "/{id}/menu";

    // 代理请求草稿接口
    static final String PAPER_DRAFT = "/{id}/paper/draft";
    static final String ONE_PAPER_DRAFT = "/{id}/paper/draft/{mediaId}";

    // 代理请求发布接口
    static final String PAPER = "/{id}/paper";
    static final String ONE_PAPER = "/{id}/paper/{articleId}";

    // 代理请求素材接口
    static final String ASSETS = "/{id}/asset";
    static final String MEDIA_ASSET = "/{id}/asset/{mediaId}";

    // 代理请求用户管理接口
    static final String USER = "/{id}/user";
    static final String ONE_USER = "/{id}/user/{userId}";
    static final String LABEL = "/{id}/label";
    static final String ONE_LABEL = "/{id}/label/{labelId}";

    // 代理模板消息/群发接口
    static final String CHAT = "/{id}/message";
    static final String BROADCAST = "/{id}/broadcast";
}
