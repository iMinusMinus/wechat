package weixin.work.domain;

public enum WeixinUrl {

    GET_ACCESS_TOKEN("/cgi-bin/gettoken") {
        @Override
        public String getQueryString() {
            return "corpid={corpId}&corpsecret={corpSecret}";
        }
    },
    // TODO
    ;

    private final String path;

    WeixinUrl(String path) {
        this.path = path;
    }

    public String getQueryString() {
        return "access_token={accessToken}";
    }

    public String getUtl() {
        return "https://qyapi.weixin.qq.com" + path + "?" + getQueryString();
    }
}
