package weixin.mp.infrastructure.rpc;

/**
 * 微信接口地址
 *
 * @author iMinusMinus
 * @date 2022-11-19
 */
public enum WeixinUrl {

    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Get_access_token.html">微信公众号和小程序使用相同的接口获取accessToken</a>
     */
    GET_ACCESS_TOKEN("/cgi-bin/token") {
        @Override
        public String getQueryString() {
            return "grant_type=client_credential&appid={appId}&secret={appSecret}";
        }
    },
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Template_Message_Interface.html#2">设置所属行业</a>
     */
    SET_INDUSTRY("/cgi-bin/template/api_set_industry"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Template_Message_Interface.html#2">获取所属行业</a>
     */
    GET_INDUSTRY("/cgi-bin/template/get_industry"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Account_Management/Generating_a_Parametric_QR_Code.html">生成带参数二维码</a>
     */
    RETRIEVE_QUICK_RESPONSE_CODE_TICKET("/cgi-bin/qrcode/create"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Account_Management/Generating_a_Parametric_QR_Code.html">通过ticket换取二维码</a>
     */
    RETRIEVE_QUICK_RESPONSE_CODE("/cgi-bin/showqrcode") {
        @Override
        public String getQueryString() {
            return "ticket={ticket}";
        }
        @Override
        public String getUrl() {
            return "https://mp.weixin.qq.com" + this.getPath() + "?" + getQueryString();
        }
    },
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Account_Management/KEY_Shortener.html">短 key 托管</a>
     */
    GEN_SHORT_URL("/cgi-bin/shorten/gen"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Account_Management/KEY_Shortener.html">短 key 托管</a>
     */
    RESTORE_SHORT_URL("/cgi-bin/shorten/fetch"),

    CLEAR_API_QUOTA("/cgi-bin/clear_quota"),
    GET_API_QUOTA("/cgi-bin/openapi/quota/get"),

    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Creating_Custom-Defined_Menu.html">创建菜单</a>
     */
    CREATE_MENU("/cgi-bin/menu/create"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Querying_Custom_Menus.html">查询菜单</a>
     */
    GET_MENU("/cgi-bin/get_current_selfmenu_info"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Deleting_Custom-Defined_Menu.html">删除所有菜单</a>
     */
    DELETE_MENU("/cgi-bin/menu/delete"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Personalized_menu_interface.html">创建个性化菜单</a>
     */
    CREATE_CUSTOM_MENU("/cgi-bin/menu/addconditional"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Personalized_menu_interface.html">删除个性化菜单</a>
     */
    DELETE_CUSTOM_MENU("/cgi-bin/menu/delconditional"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Personalized_menu_interface.html">测试个性化菜单匹配结果</a>
     */
    TEST_CUSTOM_MENU("/cgi-bin/menu/trymatch"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Getting_Custom_Menu_Configurations.html">获取自定义菜单</a>
     */
    GET_CUSTOM_MENU("/cgi-bin/menu/get"),

    GET_TEMPLATE_ID("/cgi-bin/template/api_add_template"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Template_Message_Interface.html#2">获取模板列表</a>
     */
    GET_ALL_TEMPLATES("/cgi-bin/template/get_all_private_template"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Template_Message_Interface.html#2">删除模板</a>
     */
    DELETE_TEMPLATE("/cgi-bin/template/del_private_template"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Template_Message_Interface.html#2">发送模板消息</a>
     */
    SEND_TEMPLATE_MESSAGE("/cgi-bin/message/template/send"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/One-time_subscription_info.html">推送订阅模板消息给到授权微信用户</a>
     */
    PUSH_TEMPLATE_MESSAGE_TO_SUBSCRIBERS("/cgi-bin/message/template/subscribe"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html">上传图文消息内的图片获取URL</a>
     */
    UPLOAD_IMG("/cgi-bin/media/uploadimg"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html">上传图文消息素材</a>
     */
    UPLOAD_ARTICLE("/cgi-bin/media/uploadnews"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Asset_Management/New_temporary_materials.html">新增临时素材</a>
     */
    UPLOAD_MATERIAL("/cgi-bin/media/upload") {
        @Override
        public String getQueryString() {
            return "access_token={accessToken}&type={type}";
        }
    },
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Asset_Management/Get_temporary_materials.html">获取临时素材</a>
     */
    DOWNLOAD_MEDIA("/cgi-bin/media/get") {
        @Override
        public String getQueryString() {
            return "access_token={accessToken}&media_id={mediaId}";
        }
    },
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Asset_Management/Adding_Permanent_Assets.html">添加永久素材</a>
     */
    UPLOAD_PERMANENT_MATERIAL("/cgi-bin/material/add_material") {
        @Override
        public String getQueryString() {
            return "access_token={accessToken}&type={type}";
        }
    },
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Comments_management/Image_Comments_Management_Interface.html">新增永久图文素材</a>
     */
    @Deprecated
    ADD_PERMANENT_NEWS("/cgi-bin/material/add_news"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Asset_Management/Getting_Permanent_Assets.html">获取永久素材</a>
     */
    GET_PERMANENT_MATERIAL("/cgi-bin/material/get_material"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Asset_Management/Deleting_Permanent_Assets.html">删除永久素材</a>
     */
    REMOVE_PERMANENT_MATERIAL("/cgi-bin/material/del_material"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Asset_Management/Get_the_total_of_all_materials.html">获取永久素材总数（包含非接口上传的永久素材）</a>
     */
    COUNT_PERMANENT_MATERAIL("/cgi-bin/material/get_materialcount"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Asset_Management/Get_materials_list.html">获取永久素材列表</a>
     */
    LIST_PERMANENT_MATERIAL("/cgi-bin/material/batchget_material"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html">根据标签群发</a>
     */
    PUBLISH_TO_GROUP("/cgi-bin/message/mass/sendall"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html">根据用户id群发</a>
     */
    PUBLISH_TO_PEERS("/cgi-bin/message/mass/send"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html">删除群发</a>
     */
    CANCEL_PUBLISH("/cgi-bin/message/mass/delete"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html">预览</a>
     */
    PREVIEW_PUBLISH("/cgi-bin/message/mass/preview"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html">查询群发消息发送状态</a>
     */
    GET_PUBLISH_STATUS("/cgi-bin/message/mass/get"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html">获取群发速度</a>
     */
    GET_PUBLISH_SPEED("/cgi-bin/message/mass/speed/get"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html">设置群发速度</a>
     */
    SET_PUBLISH_SPEED("/cgi-bin/message/mass/speed/set"),

    GET_AUTO_REPLY("/cgi-bin/get_current_autoreply_info"),

    SET_SUBSCRIBE_NOTIFICATION_TEMPLATE("/wxaapi/newtmpl/addtemplate"),
    DELETE_SUBSCRIBE_NOTIFICATION_TEMPLATE("/wxaapi/newtmpl/deltemplate"),

    GET_CATEGORY("/wxaapi/newtmpl/getcategory"),
    GET_PUBLIC_TEMPLATE_KEYWORDS("/wxaapi/newtmpl/getpubtemplatekeywords"),
    GET_PUBLIC_TEMPLATE("/wxaapi/newtmpl/getpubtemplatetitles"),
    GET_TEMPLATES("/wxaapi/newtmpl/gettemplate"),

    SEND_SUBSCRIBE_NOTIFICATION("/cgi-bin/message/subscribe/bizsend"),

    ADD_CUSTOM_SERVICE_ACCOUNT("/customservice/kfaccount/add"),
    INVITE_CUSTOM_SERVICE_BIND_ACCOUNT("/customservice/kfaccount/inviteworker"),
    UPDATE_CUSTOM_SERVICE_ACCOUNT("/customservice/kfaccount/update"),
    DELETE_CUSTOM_SERVICE_ACCOUNT("/customservice/kfaccount/del"),
    SET_CUSTOM_SERVICE_AVATAR("/customservice/kfaccount/uploadheadimg") {
        @Override
        public String getQueryString() {
            return "access_token={accessToken}&kf_account={account}";
        }
    },
    GET_CUSTOM_SERVICE_ACCOUNTS("/cgi-bin/customservice/getkflist"),
    SEND_CUSTOM_SERVICE_MESSAGE("/cgi-bin/message/custom/send"),
    DISPLAY_CUSTOM_SERVICE_TYPING_STATUS("/cgi-bin/message/custom/typing"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Draft_Box/Add_draft.html">新建草稿</a>
     */
    CREATE_DRAFT("/cgi-bin/draft/add"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Draft_Box/Get_draft.html">获取草稿</a>
     */
    RETRIEVE_ONE_DRAFT("/cgi-bin/draft/get"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Draft_Box/Delete_draft.html">删除草稿</a>
     */
    THROW_DRAFT_TO_TRASH("/cgi-bin/draft/delete"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Draft_Box/Update_draft.html">修改草稿</a>
     */
    REVISE_DRAFT("/cgi-bin/draft/update"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Draft_Box/Count_drafts.html">获取草稿总数</a>
     */
    COUNT_DRAFT("/cgi-bin/draft/count"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Draft_Box/Get_draft_list.html">获取草稿列表</a>
     */
    RETRIEVE_MANY_DRAFT("/cgi-bin/draft/batchget"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Publish/Publish.html">草稿发布成文</a>
     */
    PUBLISH("/cgi-bin/freepublish/submit"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Publish/Get_status.html">获取发布状态</a>
     */
    POLL_PUBLISH_STATUS("/cgi-bin/freepublish/get"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Publish/Delete_posts.html">删除发布</a>
     */
    REMOVE_PUBLISHED("/cgi-bin/freepublish/delete"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Publish/Get_article_from_id.html">获取已发布文章</a>
     */
    RETRIEVE_PUBLISHED("/cgi-bin/freepublish/getarticle"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/Publish/Get_publication_records.html">获取成功发布列表</a>
     */
    GET_PUBLISHED_ARTICLES("/cgi-bin/freepublish/batchget"),

    ENABLE_COMMENT("/cgi-bin/comment/open"),
    DISABLE_COMMENT("/cgi-bin/comment/close"),
    VIEW_COMMENTS("/cgi-bin/comment/list"),
    MARK_COMMENT_AS_SELECTED("/cgi-bin/comment/markelect"),
    UNMARK_SELECTED_COMMENT("/cgi-bin/comment/unmarkelect"),
    REMOVE_COMMENT("/cgi-bin/comment/delete"),
    REPLY_COMMENT("/cgi-bin/comment/reply/add"),
    REMOVE_REPLY("/cgi-bin/comment/reply/delete"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/User_Tag_Management.html">创建标签</a>
     */
    CREATE_LABEL("/cgi-bin/tags/create"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/User_Tag_Management.html">获取已创建标签</a>
     */
    RETRIEVE_LABEL("/cgi-bin/tags/get"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/User_Tag_Management.html">编辑标签</a>
     */
    REVISE_LABEL("/cgi-bin/tags/update"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/User_Tag_Management.html">删除标签</a>
     */
    REMOVE_LABEL("/cgi-bin/tags/delete"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/User_Tag_Management.html">获取标签下粉丝列表</a>
     */
    LIST_LABELED_USERS("/cgi-bin/user/tag/get"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/User_Tag_Management.html">批量为用户打标签</a>
     */
    LABELING("/cgi-bin/tags/members/batchtagging"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/User_Tag_Management.html">批量为用户取消标签</a>
     */
    UNLABELING("/cgi-bin/tags/members/batchuntagging"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/User_Tag_Management.html">获取用户的所有标签</a>
     */
    LABELED("/cgi-bin/tags/getidlist"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/Configuring_user_notes.html">设置用户备注名</a>
     */
    REMARK_FANS("/cgi-bin/user/info/updateremark"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/Get_users_basic_information_UnionID.html#UinonId">获取用户基本信息</a>
     */
    RETRIEVE_FANS_INFO("/cgi-bin/user/info") {
        @Override
        public String getQueryString() {
            return "access_token={accessToken}&openid={openId}&lang={language}";
        }
    },
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/Get_users_basic_information_UnionID.html#UinonId">批量获取用户基本信息</a>
     */
    BATCH_RETRIEVE_FANS("/cgi-bin/user/info/batchget"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/Getting_a_User_List.html">获取用户列表</a>
     */
    RETRIEVE_FANS_ID("/cgi-bin/user/get") {
        @Override
        public String getQueryString() {
            return "access_token={accessToken}&next_openid={nextOpenId}";
        }
    },
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/Manage_blacklist.html">拉黑用户</a>
     */
    BLOCK_FANS("/cgi-bin/tags/members/batchblacklist"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/Manage_blacklist.html">获取黑名单列表</a>
     */
    LIST_BLOCKED_FANS("/cgi-bin/tags/members/getblacklist"),
    /**
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/User_Management/Manage_blacklist.html">取消拉黑</a>
     */
    UNBLOCK_FANS("/cgi-bin/tags/members/batchunblacklist"),
    ;

    private final String path;

    public String getPath() {
        return path;
    }

    protected String getQueryString() {
        return "access_token={accessToken}";
    }

    public String getUrl() {
        return "https://api.weixin.qq.com" + path + "?" + getQueryString();
    }

    WeixinUrl(String path) {
        this.path = path;
    }
}
