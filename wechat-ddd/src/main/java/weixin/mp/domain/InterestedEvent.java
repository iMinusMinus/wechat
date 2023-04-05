package weixin.mp.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Receiving_event_pushes.html">接收的事件推送</a>
 * <a href="https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Custom_Menu_Push_Events.html">进入/点击自定义菜单的事件推送</a>
 * <a href="https://developers.weixin.qq.com/doc/offiaccount/Account_Management/Wechat_Accreditation_Event_Push.html">微信认证事件推送</a>
 * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Template_Message_Interface.html">模板消息事件推送</a>
 * <a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Batch_Sends_and_Originality_Checks.html">群发结果事件推送</a>
 * <a href="https://developers.weixin.qq.com/doc/offiaccount/Publish/Callback_on_finish.html">发布结果事件推送</a>
 */
public interface InterestedEvent extends RequestMessage {

    String EVENT_TAG = "Event";

    @Override
    default String msgId() {
        return fromOpenId() + timestamp();
    }

    @Override
    default String msgDataId() {
        return null;
    }

    @Override
    default MessageType msgType() {
        return MessageType.EVENT;
    }

    EventType eventType();

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     */
    record UnsubscribeEvent(String fromOpenId, String toAccount, long timestamp) implements InterestedEvent {

        @Override
        public EventType eventType() {
            return EventType.UNSUBSCRIBE;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param eventKey 事件 KEY 值，qrscene_为前缀，后面为二维码的参数值
     * @param ticket 二维码的ticket，可用来换取二维码图片
     */
    record SubscribeEvent(String fromOpenId, String toAccount, long timestamp,
                          String eventKey, String ticket) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.SUBSCRIBE;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param eventKey 事件 KEY 值，是一个32位无符号整数，即创建二维码时的二维码scene_id
     * @param ticket 二维码的ticket，可用来换取二维码图片
     */
    record ScanEvent(String fromOpenId, String toAccount, long timestamp,
                     String eventKey, String ticket) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.SCAN;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param longitude 地理位置经度
     * @param latitude 地理位置纬度
     * @param precision 地理位置精度
     */
    record LocationEvent(String fromOpenId, String toAccount, long timestamp,
                         BigDecimal longitude, BigDecimal latitude, BigDecimal precision) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.LOCATION;
        }
    }

    /**
     * @param fromOpenId 接收模板消息的用户的openid
     * @param toAccount 公众号微信号
     * @param timestamp 消息创建时间 （整型）
     * @param msgId 消息id
     * @param status 发送状态: success, 成功; failed:user block, 用户拒绝接收; failed: system failed, 发送失败
     */
    record TemplateMsgPushResultEvent(String fromOpenId, String toAccount, long timestamp,
                                      String msgId, String status) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.TEMPLATE_MSG_PUSH_RESULT;
        }
    }

    /**
     * TODO
     */
    record BatchSendResultEvent(String fromOpenId, String toAccount, long timestamp) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.BATCH_SEND_RESULT;
        }
    }

    record ArticlePublishResultEvent(String fromOpenId, String toAccount, long timestamp) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.ARTICLE_PUBLISH_RESULT;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param eventKey 事件 KEY 值，与自定义菜单接口中 KEY 值对应
     */
    record MenuClickEvent(String fromOpenId, String toAccount, long timestamp,
                          String eventKey) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.MENU_CLICKED;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param eventKey 事件 KEY 值，设置的跳转URL
     * @param menuId 指菜单ID，如果是个性化菜单，则可以通过这个字段，知道是哪个规则的菜单被点击了
     */
    record MenuViewEvent(String fromOpenId, String toAccount, long timestamp,
                         String eventKey, String menuId) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.MENU_VIEW;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param eventKey 事件 KEY 值，由开发者在创建菜单时设定
     * @param scanType 扫描类型，一般是qrcode
     * @param scanResult 扫描结果，即二维码对应的字符串信息
     */
    record MenuScanEvent(String fromOpenId, String toAccount, long timestamp,
                         String eventKey, String scanType, String scanResult) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.MENU_SCAN;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param eventKey 事件 KEY 值，由开发者在创建菜单时设定
     * @param scanType 扫描类型，一般是qrcode
     * @param scanResult 扫描结果，即二维码对应的字符串信息
     */
    record MenuScanningEvent(String fromOpenId, String toAccount, long timestamp,
                             String eventKey, String scanType, String scanResult) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.MENU_SCANNING;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param eventKey 事件 KEY 值，由开发者在创建菜单时设定
     * @param digests 图片的MD5值，开发者若需要，可用于验证接收到图片
     */
    record MenuPhotoEvent(String fromOpenId, String toAccount, long timestamp,
                          String eventKey, List<String> digests) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.MENU_PHOTO;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param eventKey 事件 KEY 值，由开发者在创建菜单时设定
     * @param digests 图片的MD5值，开发者若需要，可用于验证接收到图片
     */
    record MenuPictureEvent(String fromOpenId, String toAccount, long timestamp,
                            String eventKey, List<String> digests) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.MENU_PICTURE;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param eventKey 事件 KEY 值，由开发者在创建菜单时设定
     * @param digests 图片的MD5值，开发者若需要，可用于验证接收到图片
     */
    record MenuAlbumEvent(String fromOpenId, String toAccount, long timestamp,
                          String eventKey, List<String> digests) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.MENU_ALBUM;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param latitude X坐标信息(纬度)
     * @param longitude Y坐标信息(经度)
     * @param scale 精度，可理解为精度或者比例尺、越精细的话 scale越高
     * @param address 地理位置的字符串信息
     * @param poiName 朋友圈 POI 的名字，可能为空
     */
    record MenuLocatingEvent(String fromOpenId, String toAccount, long timestamp,
                             BigDecimal latitude, BigDecimal longitude, BigDecimal scale, String address,
                             String poiName) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.MENU_LOCATING;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param eventKey 事件 KEY 值，跳转的小程序路径
     * @param menuId 菜单ID，如果是个性化菜单，则可以通过这个字段，知道是哪个规则的菜单被点击了
     */
    record MenuAppletEvent(String fromOpenId, String toAccount, long timestamp,
                           String eventKey, String menuId) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.MENU_VIEW_APPLET;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID，此时发送方是系统帐号）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param expireAt 有效期 (整型)，指的是时间戳，将于该时间戳认证过期
     */
    record CertificationSuccessfulEvent(String fromOpenId, String toAccount, long timestamp,
                                        long expireAt) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.QUALIFICATION_VERIFY_SUCCESS;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID，此时发送方是系统帐号）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param failTime 失败发生时间 (整型)，时间戳
     * @param failReason 认证失败的原因
     */
    record CertificationFailedEvent(String fromOpenId, String toAccount, long timestamp,
                                    long failTime, String failReason) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.QUALIFICATION_VERIFY_FAIL;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID，此时发送方是系统帐号）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param expireAt 有效期 (整型)，指的是时间戳，将于该时间戳认证过期
     */
    record TitleReviewSuccessfulEvent(String fromOpenId, String toAccount, long timestamp,
                                      long expireAt) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.NAMING_VERIFY_SUCCESS;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID，此时发送方是系统帐号）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param failTime 失败发生时间 (整型)，时间戳
     * @param failReason 认证失败的原因
     */
    record TitleReviewFailedEvent(String fromOpenId, String toAccount, long timestamp,
                                  long failTime, String failReason) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return EventType.NAMING_VERIFY_FAIL;
        }
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID，此时发送方是系统帐号）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param expired 年审是否过期
     * @param expireAt 有效期 (整型)，指的是时间戳，年审将于/已于该时间戳认证过期
     */
    record AnnualRenewEvent(String fromOpenId, String toAccount, long timestamp,
                            boolean expired, long expireAt) implements InterestedEvent {
        @Override
        public EventType eventType() {
            return expired ? EventType.ANNUAL_VERIFY_EXPIRED : EventType.ANNUAL_RENEW;
        }
    }
}
