package weixin.mp.domain;

import java.util.List;

public interface RequestMessage extends AbstractMessage {

    @Override
    default String from() {
        return fromOpenId();
    }

    String fromOpenId();

    @Override
    default String to() {
        return toAccount();
    }

    String toAccount();

    default MessageType msgType() {
        String sn = getClass().getSimpleName();
        return MessageType.getInstance(sn.substring(0, sn.length() - "Message".length()).toLowerCase());
    }

    String msgId();

    String msgDataId();

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param msgId 消息id，64位整型
     * @param msgDataId 消息的数据ID（消息如果来自文章时才有）
     * @param content 文本消息内容
     * @param idx 多图文时第几篇文章，从1开始（消息如果来自文章时才有）
     */
    record TextMessage(String fromOpenId, String toAccount, long timestamp, String msgId, String msgDataId, String content,
                       List<String> idx) implements RequestMessage {
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param msgId 消息id，64位整型
     * @param msgDataId 消息的数据ID（消息如果来自文章时才有）
     * @param picUrl 图片链接（由系统生成）
     * @param idx 多图文时第几篇文章，从1开始（消息如果来自文章时才有）
     */
    record ImageMessage(String fromOpenId, String toAccount, long timestamp, String msgId, String msgDataId,
                        String picUrl, String mediaId, List<String> idx) implements RequestMessage {
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param msgId 消息id，64位整型
     * @param msgDataId 消息的数据ID（消息如果来自文章时才有）
     * @param mediaId 语音消息媒体id，可以调用获取临时素材接口拉取数据
     * @param format 语音格式，如amr，speex等
     * @param idx 多图文时第几篇文章，从1开始（消息如果来自文章时才有）
     */
    record VoiceMessage(String fromOpenId, String toAccount, long timestamp, String msgId, String msgDataId,
                        String mediaId, String format, List<String> idx) implements RequestMessage {
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param msgId 消息id，64位整型
     * @param msgDataId 消息的数据ID（消息如果来自文章时才有）
     * @param mediaId 语音消息媒体id，可以调用获取临时素材接口拉取数据
     * @param thumbMediaId 视频消息缩略图的媒体id，可以调用多媒体文件下载接口拉取数据
     * @param idx 多图文时第几篇文章，从1开始（消息如果来自文章时才有）
     */
    record VideoMessage(String fromOpenId, String toAccount, long timestamp, String msgId, String msgDataId,
                        String mediaId, String thumbMediaId, List<String> idx) implements RequestMessage {
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param msgId 消息id，64位整型
     * @param msgDataId 消息的数据ID（消息如果来自文章时才有）
     * @param mediaId 语音消息媒体id，可以调用获取临时素材接口拉取数据
     * @param thumbMediaId 视频消息缩略图的媒体id，可以调用多媒体文件下载接口拉取数据
     * @param idx 多图文时第几篇文章，从1开始（消息如果来自文章时才有）
     */
    record VideoletMessage(String fromOpenId, String toAccount, long timestamp, String msgId, String msgDataId,
                        String mediaId, String thumbMediaId, List<String> idx) implements RequestMessage {
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param msgId 消息id，64位整型
     * @param msgDataId 消息的数据ID（消息如果来自文章时才有）
     * @param longitude 地理位置经度
     * @param latitude 地理位置纬度
     * @param scale 地图缩放大小
     * @param label 地理位置信息
     * @param idx 多图文时第几篇文章，从1开始（消息如果来自文章时才有）
     */
    record LocationMessage(String fromOpenId, String toAccount, long timestamp, String msgId, String msgDataId,
                           String longitude, String latitude, int scale, String label, List<String> idx) implements RequestMessage {
    }

    /**
     * @param fromOpenId 发送方帐号（一个OpenID）
     * @param toAccount 开发者微信号
     * @param timestamp 消息创建时间 （整型）
     * @param msgId 消息id，64位整型
     * @param msgDataId 消息的数据ID（消息如果来自文章时才有）
     * @param title 消息标题
     * @param description 消息描述
     * @param url 消息链接
     * @param idx 多图文时第几篇文章，从1开始（消息如果来自文章时才有）
     */
    record LinkMessage(String fromOpenId, String toAccount, long timestamp, String msgId, String msgDataId,
                       String title, String description, String url, List<String> idx) implements RequestMessage {
    }

}
