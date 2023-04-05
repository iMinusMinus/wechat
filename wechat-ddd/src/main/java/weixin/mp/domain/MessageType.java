package weixin.mp.domain;

public enum MessageType {

    TEXT("text", true, true),
    IMAGE("image", true, true),
    VOICE("voice", true, true),
    VIDEO("video", true, true),
    VIDEOLET("short_video", true, false),
    LOCATION("location", true, false),
    LINK("link", true, false),
    EVENT("event", true, false),
    MUSIC("music", false, true),
    NEWS("news", false, true),
    // 以下消息类型仅客服发送
    MP_NEWS("mpnews", false, true),
    MP_VIDEO("mpvideo", false, true),
    MP_NEWS_ARTICLE("mpnewsarticle", false, true),
    MENU("msgmenu", false, true),
    CARD("wxcard", false, true),
    PAGELET("miniprogrampage", false, true),

    ;

    private final String value;

    private final boolean presentOnRequest;

    private final boolean presentOnResponse;

    MessageType(String value, boolean presentOnRequest, boolean presentOnResponse) {
        this.value = value;
        this.presentOnRequest = presentOnRequest;
        this.presentOnResponse = presentOnResponse;
    }

    public String getValue() {
        return value;
    }

    public boolean isPresentOnRequest() {
        return presentOnRequest;
    }

    public boolean isPresentOnResponse() {
        return presentOnResponse;
    }

    public static MessageType getInstance(String value) {
        for (MessageType instance : MessageType.values()) {
            if (instance.value.equals(value)) {
                return instance;
            }
        }
        return null;
    }
}
