package weixin.pay.domain;

/**
 * 微信支付URL
 */
public enum WeixinUrl {

    PAY_JSAPI("/v3/pay/transactions/jsapi"),
    ;

    private final String path;

    WeixinUrl(String path) {
        this.path = path;
    }

    public String getUrl() {
        return "https://api.mch.weixin.qq.com" + path; // TODO
    }
}
