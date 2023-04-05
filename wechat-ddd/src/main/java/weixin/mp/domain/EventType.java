package weixin.mp.domain;

public enum EventType {

    SUBSCRIBE("subscribe", false),
    UNSUBSCRIBE("unsubscribe", false),
    SCAN("SCAN", false), // 已经关注公众号的用户扫描带场景值二维码时，则微信会将带场景值扫描事件推送给开发者
    LOCATION("LOCATION", false), // 用户同意上报地理位置后，每次进入公众号会话时，都会在进入时上报地理位置，或在进入会话后每5秒上报一次地理位置，公众号可以在公众平台网站中修改以上设置
    MENU_CLICKED("CLICK", false), // 点击菜单拉取消息时的事件推送
    MENU_VIEW("VIEW", false), // 点击菜单跳转链接时的事件推送
    MENU_SCAN(MenuType.SCAN_QUICK_RESPONSE_CODE_PUSH.getValue(), true), // 扫码推事件的事件推送
    MENU_SCANNING(MenuType.SCAN_QUICK_RESPONSE_CODE_WAITING.getValue(), true), // 扫码推事件且弹出“消息接收中”提示框的事件推送
    MENU_PHOTO(MenuType.PHOTO.getValue(), true), // 弹出系统拍照发图的事件推送
    MENU_PICTURE(MenuType.IMAGE_SELECT.getValue(), false), // 弹出拍照或者相册发图的事件推送
    MENU_ALBUM(MenuType.ALBUM.getValue(), true), // 弹出微信相册发图器的事件推送
    MENU_LOCATING(MenuType.LOCATE.getValue(), true), // 弹出地理位置选择器的事件推送
    MENU_VIEW_APPLET("view_miniprogram", false), // 点击菜单跳转小程序的事件推送

    TEMPLATE_MSG_PUSH_RESULT("TEMPLATESENDJOBFINISH", false), // 在模版消息发送任务完成后，微信服务器会将是否送达成功作为通知

    BATCH_SEND_RESULT("MASSSENDJOBFINISH", false), // 群发结果推送

    ARTICLE_PUBLISH_RESULT("PUBLISHJOBFINISH", false), // 发布结果推送

    QUALIFICATION_VERIFY_SUCCESS("qualification_verify_success", false), // 资质认证成功
    QUALIFICATION_VERIFY_FAIL("qualification_verify_fail", false), // 资质认证失败
    NAMING_VERIFY_SUCCESS("naming_verify_success", false), // 名称认证成功
    NAMING_VERIFY_FAIL("naming_verify_fail", false), // 名称认证失败
    ANNUAL_RENEW("annual_renew", false), // 年审通知
    ANNUAL_VERIFY_EXPIRED("verify_expired", false), // 认证过期失效通知
    ;

    private final String value;

    private final boolean allowReplyMessage; // TODO，待验证

    public String getValue() {
        return value;
    }

    public boolean isAllowReplyMessage() {
        return allowReplyMessage;
    }

    EventType(String value, boolean allowReplyMessage) {
        this.value = value;
        this.allowReplyMessage = allowReplyMessage;
    }

    public static EventType getInstance(String value) {
        for (EventType instance : EventType.values()) {
            if (instance.value.equals(value)) {
                return instance;
            }
        }
        return null;
    }
}
