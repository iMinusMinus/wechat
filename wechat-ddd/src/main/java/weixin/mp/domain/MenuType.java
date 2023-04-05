package weixin.mp.domain;

public enum MenuType {

    /**
     * 二级菜单容器
     */
    BOX("sub_button", true, true),


    // ----- 以下菜单仅可在微信公众号管理页面 -----
    /**
     * 图文消息
     */
    NEWS("news", true, false),
    /**
     * 图片消息
     */
    IMAGE("img", true, false),
    /**
     * 音频消息
     */
    VOICE("voice", true, false),
    /**
     * 视频消息
     */
    VIDEO("video", true, false),
    /**
     * 视频号消息
     */
    // 目前菜单可以设置，接口拉取不到


    // ----- 以下菜单仅即可微信公众号管理页面设置也可API设置 -----
    /**
     * 微信客户端将会打开开发者在按钮中填写的网页URL，可与网页授权获取用户基本信息接口结合，获得用户基本信息
     */
    VIEW("view", true, true),
    /**
     * 点击跳转小程序页面。公众号可关联10个“同主体或关联主体”的小程序，3个“非同主体”小程序
     */
    PROGRAMLET("miniprogram", true, true),


    // ----- 以下菜单仅可API设置 -----
    /**
     * 微信服务器会通过消息接口推送消息类型为 event 的结构给开发者，并且带上按钮中开发者填写的 key 值
     */
    CLICK("click", false, true),
    /**
     * 微信客户端将调起扫一扫工具，完成扫码操作后显示扫描结果（如果是URL，将进入URL），且会将扫码的结果传给开发者，开发者可以下发消息
     */
    SCAN_QUICK_RESPONSE_CODE_PUSH("scancode_push", false, true),
    /**
     * 弹出“消息接收中”提示框用户点击按钮后，微信客户端将调起扫一扫工具，完成扫码操作后，将扫码的结果传给开发者，同时收起扫一扫工具，然后弹出“消息接收中”提示框，随后可能会收到开发者下发的消息
     */
    SCAN_QUICK_RESPONSE_CODE_WAITING("scancode_waitmsg", false, true),
    /**
     * 弹出系统拍照发图用户点击按钮后，微信客户端将调起系统相机，完成拍照操作后，会将拍摄的相片发送给开发者，并推送事件给开发者，同时收起系统相机，随后可能会收到开发者下发的消息
     */
    PHOTO("pic_sysphoto", false, true),
    /**
     * 弹出微信相册发图器用户点击按钮后，微信客户端将调起微信相册，完成选择操作后，将选择的相片发送给开发者的服务器，并推送事件给开发者，同时收起相册，随后可能会收到开发者下发的消息
     */
    ALBUM("pic_weixin", false, true),
    /**
     * 弹出拍照或者相册发图用户点击按钮后，微信客户端将弹出选择器供用户选择“拍照”或者“从手机相册选择”。用户选择后即走其他两种流程
     */
    IMAGE_SELECT("pic_photo_or_album", false, true),
    /**
     * 弹出地理位置选择器用户点击按钮后，微信客户端将调起地理位置选择工具，完成选择操作后，将选择的地理位置发送给开发者的服务器，同时收起位置选择工具，随后可能会收到开发者下发的消息
     */
    LOCATE("location_select", false, true),

    // ----- 给第三方平台旗下未微信认证（具体而言，是资质认证未通过）的订阅号准备的事件类型。无事件推送 -----
    /**
     * 下发消息（除文本消息）用户点击media_id类型按钮后，微信服务器会将开发者填写的永久素材 id 对应的素材下发给用户，永久素材类型可以是图片、音频、视频，永久素材 id 必须是在“素材管理/新增永久素材”接口上传后获得的合法id
     */
    NEWS1("media_id", false, true),
    /**
     * 微信客户端将会以卡片形式，下发开发者在按钮中填写的图文消息
     */
    NEWS2("article_id", false, true),
    /**
     * 微信客户端将打开开发者在按钮中填写的永久素材 id 对应的图文消息URL
     */
    NEWS3("article_view_limited", false, true),

    ;

    private final String value;

    /**
     * 微信官方页面可设置菜单类型
     */
    private final boolean adminAvailable;

    /**
     * API可设置该菜单类型
     */
    private final boolean apiAvailable;

    public String getValue() {
        return value;
    }

    public boolean isAdminAvailable() {
        return adminAvailable;
    }

    public boolean isApiAvailable() {
        return apiAvailable;
    }

    public static MenuType getInstance(String value) {
        for (MenuType instance : MenuType.values()) {
            if (instance.value.equals(value)) {
                return instance;
            }
        }
        return null;
    }

    MenuType(String value, boolean adminAvailable, boolean apiAvailable) {
        this.value = value;
        this.adminAvailable = adminAvailable;
        this.apiAvailable = apiAvailable;
    }
}
