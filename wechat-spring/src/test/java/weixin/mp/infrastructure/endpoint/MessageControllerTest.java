package weixin.mp.infrastructure.endpoint;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import weixin.Application;
import weixin.mp.infrastructure.config.SimpleConfiguration;
import weixin.mp.infrastructure.config.SpringConfiguration;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {Application.class, SpringConfiguration.class, SimpleConfiguration.class, MessageController.class},
        properties = {
        "weixin.accounts.test.appId=wxd3a0f6c8176edcab",
        "weixin.accounts.test.appSecret=9ac42770ca32920455919cd190a49c9b",
        "weixin.accounts.test.token=GreedIsGood",
        "weixin.accounts.test.mode=PLAINTEXT",

        "weixin.accounts.mp.appId=wxabb8e5e80f591861",
        "weixin.accounts.mp.appSecret=9ac42770ca32920455919cd190a49c9b",
        "weixin.accounts.mp.token=GreedIsGood",
        "weixin.accounts.mp.verify=true",
        "weixin.accounts.mp.mode=CIPHERTEXT",
        "weixin.accounts.mp.key=h7ZycbpEh2vNdfkYulfw9pG95HjLYwmrKBFeqIktff6",
})
public class MessageControllerTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    @DisplayName("微信验证服务器")
    public void testChallenge() {
        String signature = "37cc4891f809493628677ae2b592e63c938c0b03";
        String echostr = "8641591277801631551";
        String timestamp = "1668869433";
        String nonce = "977789310";
        Map<String, String> vars = new HashMap<>();
        vars.put("signature", signature);
        vars.put("echostr", echostr);
        vars.put("timestamp", timestamp);
        vars.put("nonce", nonce);
        webClient.get()
                .uri("/test?signature={signature}&echostr={echostr}&timestamp={timestamp}&nonce={nonce}", vars)
                .header("User-Agent", "Mozilla/4.0")
                .header("Accept", "*/*")
                .header("Pragma", "no-cache")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo(echostr);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1668926536</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[subscribe]]></Event>
                    <EventKey><![CDATA[]]></EventKey>
                    </xml>
                    """
    })
    @DisplayName("用户关注")
    public void testOnSubscribe(String rawXml) {
        webClient.post().uri("/test?signature=26abcc1499ae64fa330536e83b559e1c99844e2c&timestamp=1668926536&nonce=794508645&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .header("User-Agent", "Mozilla/4.0")
                .header("Host", "127.0.0.1:20080")
                .header("Accept", "*/*")
                .header("Pragma", "no-cache")
                .header("X-Forwarded-For", "81.69.103.236")
                .contentType(MediaType.TEXT_XML)
                .contentLength(278)
                .bodyValue(rawXml).exchange().expectStatus().isOk();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1670592443</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[subscribe]]></Event>
                    <EventKey><![CDATA[qrscene_110]]></EventKey>
                    <Ticket><![CDATA[gQGP8DwAAAAAAAAAAS5odHRwOi8vd2VpeGluLnFxLmNvbS9xLzAybUdKYm9weGFjU0cxeFJVajF6MU0AAgSFN5NjAwTwAAAA]]></Ticket>
                    </xml>
                    """
    })
    @DisplayName("通过扫描二维码关注")
    public void testOnSubscribeAfterScan(String rawXml) {
        webClient.post().uri("/test?signature=eef9a7798349ab9473dc8ea661ccabc3ed47778d&timestamp=1670592443&nonce=1078156260&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(rawXml).exchange().expectStatus().isOk();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1668869541</CreateTime>
                    <MsgType><![CDATA[text]]></MsgType>
                    <Content><![CDATA[文本消息]]></Content>
                    <MsgId>23892468385179204</MsgId>
                    </xml>
                    """
    })
    @DisplayName("用户在公众号发送文本消息")
    public void testOnTxtMessage(String rawXml) {
        webClient.post().uri("/test?signature=8e54ae7d2bee997f244afeea959387250adfb889&timestamp=1668869542&nonce=973755032&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .header("User-Agent", "Mozilla/4.0")
                .header("Accept", "*/*")
                .header("Pragma", "no-cache")
                .contentType(MediaType.TEXT_XML)
                .contentLength(283)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").isEqualTo("文本消息");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml>
                        <ToUserName><![CDATA[gh_b4f0e22ae50e]]></ToUserName>
                        <FromUserName><![CDATA[oJqxQwYRyZMXFTc8sipB5TSe6Vw4]]></FromUserName>
                        <CreateTime>1669640883</CreateTime>
                        <MsgType><![CDATA[text]]></MsgType>
                        <Content><![CDATA[测试文本消息/:share©]]></Content>
                        <MsgId>23903507600338688</MsgId>
                        <Encrypt><![CDATA[XcXRgKvv9hrHKuJ27qEU5uqB2GCuM8XHRq0Kwt2CrV8MfiSba2dTDhrob5k9p3cyZp59xme5JyYl9YRDgbgUkaoA8iBvXehvjNKJbWX/YjlrfMbOmXveubd0KltgavzyjvUvh75QIf0bMSeKvZZxqGfCct/Ldlp4xyKW7FEk9WOoBask4y3yBCf683qHvkCFRvNwCjPwAdtM/c/Y/xIt1FXd+ezWFRwtgJc4X5+XmI1il6s/sX8l6w9K0nVGPsBSsG7x+cTFafR+SHCLofCX2LB6ysXtAr2/KjNwklN7TOM4FiqO8vyYVxvVoiIUZA3nhoz/N7Hvyvt3/L4XavbjcTH1sFvdTe6wwca/z2u7EA8d7bhMY4vQ4iPX42+NYU5VJb52CmFy0p6uf7VR5vm6k79Ac2eH+blWg1E3kZxMd922T/JJuEqNEuxG7+1w5EIvQOIhy9F4dO0QsXBYY3RZtQ==]]></Encrypt>
                    </xml>
                    """
    })
    @DisplayName("混合模式下用户在公众号发送文本消息")
    public void testOnMixTxtMessage(String rawXml) {
        webClient.post().uri("/mp?signature=6aab228cf5918017b302463a355bf4b4a1d97321&timestamp=1669640884&nonce=1062497198&openid=oJqxQwYRyZMXFTc8sipB5TSe6Vw4&encrypt_type=aes&msg_signature=12aae47aece96fcfbb75a6049d78e5e6e5d981d7")
                .header("User-Agent", "Mozilla/4.0")
                .header("Accept", "*/*")
                .header("Pragma", "no-cache")
                .contentType(MediaType.TEXT_XML)
                .contentLength(832)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Encrypt").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml>
                        <ToUserName><![CDATA[gh_b4f0e22ae50e]]></ToUserName>
                        <Encrypt><![CDATA[nXJsNjyg1mO1CZAvs4n6iAMSg8m3JIcmsUeWJEtr2SU/mskaPsywpBbc0wxE+vaPgTlECUnInGypLFOkvKR5vloY+55xynbMZ+DzKo1WFf7No3CkjiDD6bSA7MFuVAGNAYYDfXhhDe5bSFLmHQOOGAbEL0dtyNXXLcNAglSv6qbC94GYljypiBfRH4Wm4JnYjvaFZG4CwtFFsh80cSMIyj+qTv9zVURbcY6T6I2JNzM0txYg8hb6IMvbT1+OTNUZ2j1yqWFYsWYl7IqLCSaWPNVgwn7Nx/S8Y9au/BnXO1yy+Y0akzVvycG/BOQ3PfOkx00nryYE3O2GNz/LCVnbI0Z00ZEo3W3Ea1fWgUH7blbXl2tvjyxxTTySQYYbkiwVNeSLYYAMzjALXBaGJDi3TeiCOwwNryoq9LGWveMHbOI8Bwu4VNlxkIc28bRxMECJLv9AbcuYvriR/t6YaOCFrg==]]></Encrypt>
                    </xml>
                    """
    })
    @DisplayName("安全模式下用户在公众号发送文本消息")
    public void testOnEncryptTxtMessage(String rawXml) {
        webClient.post().uri("/mp?signature=dd5ed7952f4a30058844643e67b86823fcf0cedf&timestamp=1669731472&nonce=1487905294&openid=oJqxQwYRyZMXFTc8sipB5TSe6Vw4&encrypt_type=aes&msg_signature=2152b4cd4935c9a21b5c3a0e97b197b74d8fd65d")
                .header("User-Agent", "Mozilla/4.0")
                .header("Accept", "*/*")
                .header("Pragma", "no-cache")
                .contentType(MediaType.TEXT_XML)
                .contentLength(578)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/MsgType").doesNotExist();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1668872148</CreateTime>
                    <MsgType><![CDATA[image]]></MsgType>
                    <PicUrl><![CDATA[http://mmbiz.qpic.cn/mmbiz_jpg/fNVGiboVppvsbCrxzUJXWM77jKZxevAIzb5AKlvMZ0DAUmTuicVZMPTowaL8ERsSxxrhl0gzH4J4JicxcAJ6FW3mw/0]]></PicUrl>
                    <MsgId>23892506037395301</MsgId>
                    <MediaId><![CDATA[r17pubpoqgAFxSEC15N0DcUMMAnhMTvYYPCwxw_NRvI-EFkPSTO8yUYVet3oKHJd]]></MediaId>
                    </xml>
                    """
    })
    public void testOnPicMessage(String rawXml) {
        webClient.post().uri("/test?signature=04471d1bbfcdae87fb419771aff53f0cd7e9b9dd&timestamp=1668872149&nonce=748225602&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/MsgType").isEqualTo("image");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml>
                        <ToUserName><![CDATA[gh_b4f0e22ae50e]]></ToUserName>
                        <FromUserName><![CDATA[oJqxQwYRyZMXFTc8sipB5TSe6Vw4]]></FromUserName>
                        <CreateTime>1669641198</CreateTime>
                        <MsgType><![CDATA[image]]></MsgType>
                        <PicUrl><![CDATA[http://mmbiz.qpic.cn/mmbiz_jpg/fNVGiboVppvtIGNcZzORUp3qQaJicU5CInwoicr12aRqFn5gxQoGGMI76VzKUDYdT2Y0dD0Gl1UOwL7Y69M0u4q4Q/0]]></PicUrl>
                        <MsgId>23903513693085294</MsgId>
                        <MediaId><![CDATA[C37DwkADiUyieje_3i09HwITq4cQXvLlc6uuH8NvY-u3viSOzaSlwjLJqdDbghav]]></MediaId>
                        <Encrypt><![CDATA[nhFXpT/Aa9Tvc2i67/pEOAD35cl2I5ellXhyZG0XPjobK/NpbFIhbpxEUdfor+6cWRuqBzfm1ySSAZM++UTuX52Dub2rBJRtZJJ1JhL4NpUfIPlkdBjr5Ko20xGqpzuUMqnJqS5HP9i/Ogmq/I86Vxoox10bnZDJHt7aUK+GooGrqm4FGUPsuDzPmu6nqrAhu501AhvP8ZPj2RJWhQBJXCJAlsCk1EDimeLjMSF9coCoNOSIhRFbt9YqwebYJMx5KV/gU3oVQTN+900dLuUutGw/3UcD0jfoIVtkH7hrLqOPGDUlSINo9rjaP60MSjqP2wf8+bCpHlYAjK02y8A7Pks02tILRMvsQgh+ukIXB82dE3h90679aH38+aEkPD276oHKfUia08WOGu4vuOR3vr7dqVS5vsQSUu8NnPV/L6NYySmwyYZ+8s5UhW3yezJfhp5WcWm6qbFkgR5y9PZOj1zdv9AzckhE+6lBOY2G6hIMCwwtaeCmpB2G05oY39cGMWMM1PynbB79CXa9nQ4hiUzYZ7ikyJ8+5Aii0LDUHq9Wl6pLpnk7mIhOE2sICpR9nfHYBtmxVRKDGRqECFahe+ozC4kGIVs8Xz3XMPnMfhTggCYI10KCaU0orUO2Ldo8VgkAsW07l0V2NhxbCXLCCJilEOYWuon9QvHX4MEYZrdAzHivhtUZGrA87SwrqP/0MehmR2ngQLtCVjMoUqrbCA==]]></Encrypt>
                    </xml>
                    """
    })
    public void testOnMixPicMessage(String rawXml) {
        webClient.post().uri("/mp?signature=dea93f6823a5a8882b7e5f43a944f56a23fedb7b&timestamp=1669641199&nonce=1174911539&openid=oJqxQwYRyZMXFTc8sipB5TSe6Vw4&encrypt_type=aes&msg_signature=c451d1120b507ecc26b409d3e4a29e8772328cb6")
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Encrypt").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                     <xml>
                        <ToUserName><![CDATA[gh_b4f0e22ae50e]]></ToUserName>
                        <Encrypt><![CDATA[Yft2lEtyysnbWgRBojm7QvQfyw0puTZjbNsg5PqX8Dk+VqPLywqQlKfVVHJ/wfuusgeE3jfVQkTBUjY0aVjuaqMOC+4VWO5hiTysIG+Uv+g9Cdern/Wy3+Yur1mcfMU2sIx+6cksVaw7Gj03E2Dc7O2U/WVqOnBWmVxtT4IjU3Ke7Crku6D6by3ozNQhDMJJJZBnjwGy/MGL5mwGOZo6vlmBo5x+Z2TDnPjQWOQTjwnyrDYUrUkdDx/42Y1ObiYEeVS/LTuaCOtH34Rhz0swgadO3Xs0Dlpr93Bywp+IBViezkK+QTtPyQJFquE/e6H2iilrQpz7S8VeVJmcW9Dt1dpHMihGI9vdAnuL67YhUJM0ssytyNMPI+9MXakpFMXr4jEdpER+wFz/tGk78WKhBh9DVwYOA1/3QD6xaqhUuY5/6j8zo4nQQjzhzOKd9m9ktzDQfNUemjHrRjRKFW8Pw9IFwfDpFMBqpPpOt/3o2cO2x22JrVT5LMPfwT+qZheIaT4ILHLyt8/R5Lu+jkml7O+aK6B8qxTRsju0+wt4oggfij26edXnsjuDkdeeqX6o6vy82zpCoPEBtolc0iDl45c+qCTzSgzItpm37XwS+YLeZSyk0wvyFglk/Flv8wlLhY406HOM1RyfpSweAil8+pb16UwxSc2cR8FQ8VV1583CC0ofFABwdLzhljd3l+oJO8zD/Zl6CYH/y+CRPqFDFQ==]]></Encrypt>
                    </xml>
                    """
    })
    @DisplayName("安全模式下用户在公众号发送图片消息")
    public void testOnEncryptPicMessage(String rawXml) {
        webClient.post().uri("/mp?signature=80dffbba37d96dfe5315922b94c5b843e002a74d&timestamp=1669646632&nonce=284404514&openid=oJqxQwYRyZMXFTc8sipB5TSe6Vw4&encrypt_type=aes&msg_signature=05667ddda8e4a25fa38736e725b89b2fc6e96cee")
                .header("User-Agent", "Mozilla/4.0")
                .header("Accept", "*/*")
                .header("Pragma", "no-cache")
                .contentType(MediaType.TEXT_XML)
                .contentLength(834)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/FromUserName").doesNotExist();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1668872383</CreateTime>
                    <MsgType><![CDATA[voice]]></MsgType>
                    <MediaId><![CDATA[r17pubpoqgAFxSEC15N0DfEpiGWN1IAIOTA8IR5IqnUL6dSen03mYFngOYxahsbo]]></MediaId>
                    <Format><![CDATA[amr]]></Format>
                    <MsgId>23892507910728562</MsgId>
                    <Recognition><![CDATA[]]></Recognition>
                    </xml>
                    """
    })
    @DisplayName("用户在公众号发送语音消息")
    public void testOnVoiceMessage(String rawXml) {
        webClient.post().uri("/test?signature=4371d88f8e2f1821b1ffa046cfa03e005e18ce99&timestamp=1668872383&nonce=477314345&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Voice/MediaId").isEqualTo("r17pubpoqgAFxSEC15N0DfEpiGWN1IAIOTA8IR5IqnUL6dSen03mYFngOYxahsbo");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1668926576</CreateTime>
                    <MsgType><![CDATA[video]]></MsgType>
                    <MediaId><![CDATA[LpJA9bW3JX2VI5qoTLP4ZOWVScSQuUg0RMGlYmDF1cH-lQo143YnzKvEjL0fbk3h]]></MediaId>
                    <ThumbMediaId><![CDATA[LpJA9bW3JX2VI5qoTLP4ZK4WesEf0KTyQVrQ2Pouhy6fSPZX2eZzWcReAu-LP_97]]></ThumbMediaId>
                    <MsgId>23893282193621731</MsgId>
                    </xml>
                    """
    })
    @DisplayName("用户在公众号发送视频消息")
    public void testOnVideoMessage(String rawXml) {
        webClient.post().uri("/test?signature=1b41cc830fd8a6ca4e30d343dff9d7b788429c90&timestamp=1668926577&nonce=2115998266&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .header("User-Agent", "Mozilla/4.0")
                .header("Connection", "close")
                .header("Accept", "*/*")
                .header("Pragma", "no-cache")
                .contentType(MediaType.TEXT_XML)
                .contentLength(442)
                .bodyValue(rawXml)
                .exchange().expectStatus().isOk()
                .expectBody().xpath("/xml/Video/MediaId").isEqualTo("LpJA9bW3JX2VI5qoTLP4ZOWVScSQuUg0RMGlYmDF1cH-lQo143YnzKvEjL0fbk3h");
    }

    @ParameterizedTest
    @ValueSource(strings = {

    })
    @Disabled
    public void testOnVideoletMessage(String rawXml) {
        // TODO
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1668931049</CreateTime>
                    <MsgType><![CDATA[location]]></MsgType>
                    <Location_X>31.237238</Location_X>
                    <Location_Y>121.666672</Location_Y>
                    <Scale>16</Scale>
                    <Label><![CDATA[浦东新区上丰路1270号]]></Label>
                    <MsgId>23893347810211482</MsgId>
                    </xml>
                    """
    })
    @DisplayName("用户在公众号发送地理位置消息")
    public void testOnGeoMessage(String rawXml) {
        webClient.post().uri("/test?signature=028af8cc0a7309d5253b4878deb244e32b5a223d&timestamp=1668931050&nonce=27055319&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .header("User-Agent", "Mozilla/4.0")
                .header("Connection", "close")
                .header("Accept", "*/*")
                .header("Pragma", "no-cache")
                .header("X-Forwarded-For", "175.24.214.150")
                .contentType(MediaType.TEXT_XML)
                .contentLength(388)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml>
                        <ToUserName><![CDATA[gh_b4f0e22ae50e]]></ToUserName>
                        <Encrypt><![CDATA[2fgWmw5AOuU96maZhG2t0ReOTijK9QMxepoe3BSUK62aGJVWbymj3JX3wd8o9XAa923BRyGiWhfLvdudQgUoIk6MboiASA0ytwx+mhgVFGSNXdNu14P+CBvsoN+DRLUUapZtHY+Qy7uaAEXg7u09k4gir5KruUxPz853PVI52IQqyVvsSRM/KdePrWQWIqE7C1rKsmX/3uHVKcRZctUZlTCT48QQ2zF7tBUzI8/by+vXuAiEUYCJ5AUUqn8B8zRaH2fpiJZ4iqssqe1HytqrCPT/8kMBaaCXSDTFh+SwubZVxTBau1H1fmS4NvvE467W0sIgUVRIH8RgyF6qaJAViX5VpDADiszMx7BDGjI56JkWjockE7H8gpVuq5Qu+NXm769O2nVC8UmUnKK+U0h7umcQN1FmcHySWK8HCgcWfjjLKdrBSXWGDxc+vUo92b0ASQCN4fTKbnz2qENok5lhkeEkAov+F1OFNsLQEyaoSvQbGBkxfIHcq55i4enIN0TGegg1sZBhNvExlznJ4U4x47C0n/VLKjhw+5HPeokBHrYSHFQsFTr9nzxlxBdCXYq4ZOoQqGo/h3uwPRCK5XJVUQ==]]></Encrypt>
                    </xml>
                    """
    })
    @DisplayName("安全模式下用户在公众号发送地理位置消息")
    public void testOnEncryptGeoMessage(String rawXml) {
        webClient.post().uri("/mp?signature=57f2f3adee68aeefe994ab63210bea59954e614c&timestamp=1669646811&nonce=1047919831&openid=oJqxQwYRyZMXFTc8sipB5TSe6Vw4&encrypt_type=aes&msg_signature=847f8561048c749f6cced15bade23cfdba202279")
                .header("User-Agent", "Mozilla/4.0")
                .header("Connection", "close")
                .header("Accept", "*/*")
                .header("Pragma", "no-cache")
                .header("X-Forwarded-For", "81.69.103.236")
                .contentType(MediaType.TEXT_XML)
                .contentLength(706)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Encrypt").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1668926928</CreateTime>
                    <MsgType><![CDATA[link]]></MsgType>
                    <Title><![CDATA[B 站高可用架构实践]]></Title>
                    <Description><![CDATA[本文整理了 B 站在云+社区沙龙分享的高可用架构，一起来学习小破站的稳定性实践吧！​]]></Description>
                    <Url><![CDATA[http://mp.weixin.qq.com/s?__biz=MzA4MjEyNTA5Mw==&mid=2652581569&idx=1&sn=8e49b50b544807b2685a7a2a12cb3b74&chksm=8465068bb3128f9d7e03bfd874d0b8a5cf1df4a9e9079c2c9fb36394af8ab41204ddbbb10379&mpshare=1&scene=24&srcid=1201NL6vCASgMuYtG2HqlNUo&sharer_sharetime=1638319262554&sharer_shareid=00a532a01f508d5bdb34695c01ff3740#rd]]></Url>
                    <MsgId>23893287037808614</MsgId>
                    </xml>
                    """
    })
    @DisplayName("用户在公众号发送链接消息（如收藏，发送音乐/文件不会产生消息，在用户界面会提示'不支持的消息类型，对方无法接收'）")
    public void testOnLinkMessage(String rawXml) {
        webClient.post().uri("/test?signature=801a713b040f362fd3c984edee60d7ff86bf7d75&timestamp=1668926929&nonce=326923315&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .header("User-Agent", "Mozilla/4.0")
                .header("Connection", "close")
                .header("Accept", "*/*")
                .header("Pragma", "no-cache")
                .header("X-Forwarded-For", "175.24.214.222")
                .contentType(MediaType.TEXT_XML)
                .contentLength(795)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1670591703</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[SCAN]]></Event>
                    <EventKey><![CDATA[110]]></EventKey>
                    <Ticket><![CDATA[gQFJ8DwAAAAAAAAAAS5odHRwOi8vd2VpeGluLnFxLmNvbS9xLzAyZVQ1d29aeGFjU0cxeUNSamh6MVAAAgS2NJNjAwTwAAAA]]></Ticket>
                    </xml>
                    """
    })
    @DisplayName("带参二维码被已关注用户扫描后收到事件推送")
    public void testOnQrCodeScan(String rawXml) {
        webClient.post().uri("/test?signature=7e160ffc904ecbfcdc2d00070e74f6ea85c62793&timestamp=1670591703&nonce=366928050&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1670159648</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[LOCATION]]></Event>
                    <Latitude>31.267813</Latitude>
                    <Longitude>121.598755</Longitude>
                    <Precision>30.000000</Precision>
                    </xml>
                    """
    })
    public void testOnGeoTracing(String rawXml) {
        webClient.post().uri("/test?signature=a351f6202d29bc890ad53615c0e6d3f2b5fd71e2&timestamp=1670159648&nonce=545741743&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1670679843</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[TEMPLATESENDJOBFINISH]]></Event>
                    <MsgID>2702600966117900288</MsgID>
                    <Status><![CDATA[success]]></Status>
                    </xml>
                    """
    })
    @DisplayName("发送模板消息后微信推送的通知")
    public void testOnTemplateMsgPushResult(String rawXml) {
        webClient.post().uri("/test?signature=46169177d5f1492c7b16efd0a2087fc762f069bf&timestamp=1670679843&nonce=1326674064&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1670337905</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[CLICK]]></Event>
                    <EventKey><![CDATA[PRACTICE_MAKES_SENSE]]></EventKey>
                    </xml>
                    """
    })
    @DisplayName("用户点击自定义菜单")
    public void testOnCustomMenuClick(String rawXml) {
        webClient.post().uri("/test?signature=39a7f0a21a39531789bdfdc78e37cf2ea347b769&timestamp=1670337905&nonce=1498394205&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1670332263</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[VIEW]]></Event>
                    <EventKey><![CDATA[https://timesmagazin.com/category/world]]></EventKey>
                    <MenuId>461376933</MenuId>
                    </xml>
                    """
    })
    @DisplayName("用户打开自定义菜单（跳转URL）")
    public void testOnCustomMenuForward(String rawXml) {
        webClient.post().uri("/test?signature=1093f79841b7385ed202febc6e27263101a5cea9&timestamp=1670332263&nonce=1314998574&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1670338145</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[scancode_push]]></Event>
                    <EventKey><![CDATA[WITHOUT_AN_INVESTIGATION_THERE_IS_NO_VOICE]]></EventKey>
                    <ScanCodeInfo><ScanType><![CDATA[qrcode]]></ScanType>
                    <ScanResult><![CDATA[https://qrcode.sh.gov.cn/enterprise/scene?f=2&m=N0MxMjgyMTQ2OTU5NzY2QTU5NzU1QzY5MUM3MTQ0RDA2QkM0QkRGNEYwMTUxRjAyRTY1OEFEMDlERkYwQzc1Ng==d2f37481472983d9d0e84e9948fa1216615178980510000000000]]></ScanResult>
                    </ScanCodeInfo>
                    </xml>
                    """
    })
    @DisplayName("用户打开自定义菜单，弹出扫描二维码")
    public void testOnCustomMenuScan(String rawXml) {
        webClient.post().uri("/test?signature=bd2153a970009d725a02ec2fcf30fdaebc533ef2&timestamp=1670338145&nonce=2071327275&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1670338338</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[scancode_waitmsg]]></Event>
                    <EventKey><![CDATA[LISTENING_IS_BRIGHT_FAITH_IS_DARK]]></EventKey>
                    <ScanCodeInfo><ScanType><![CDATA[qrcode]]></ScanType>
                    <ScanResult><![CDATA[https://qrcode.sh.gov.cn/enterprise/scene?f=2&m=N0MxMjgyMTQ2OTU5NzY2QTU5NzU1QzY5MUM3MTQ0RDA2QkM0QkRGNEYwMTUxRjAyRTY1OEFEMDlERkYwQzc1Ng==d2f37481472983d9d0e84e9948fa1216615178980510000000000]]></ScanResult>
                    </ScanCodeInfo>
                    </xml>
                    """
    })
    @DisplayName("用户打开自定义菜单，扫描二维码处理中")
    public void testOnCustomMenuScanning(String rawXml) {
        webClient.post().uri("/test?signature=5e9ab51c1f8764a67b3cd53ecae9b85e5cc81f4f&timestamp=1670338338&nonce=74117994&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1670338473</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[pic_photo_or_album]]></Event>
                    <EventKey><![CDATA[LEARN_FROM_HISTORY]]></EventKey>
                    <SendPicsInfo><Count>1</Count>
                    <PicList><item><PicMd5Sum><![CDATA[c4eb1ac953fe810e7436ca0421c92aa4]]></PicMd5Sum>
                    </item>
                    </PicList>
                    </SendPicsInfo>
                    </xml>"""
    })
    @DisplayName("用户从自定义菜单选择拍照或相册发送出图片")
    public void testOnCustomMenuPhoto(String rawXml) {
        webClient.post().uri("/test?signature=6c2794f34cd3e4693ca1be922dc137d983c8abc6&timestamp=1670338473&nonce=448075079&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1670420718</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[pic_sysphoto]]></Event>
                    <EventKey><![CDATA[LIVE_IN_THE_MOMENT]]></EventKey>
                    <SendPicsInfo><Count>1</Count>
                    <PicList><item><PicMd5Sum><![CDATA[5776aee1cd37dd33a327949a910ddd17]]></PicMd5Sum>
                    </item>
                    </PicList>
                    </SendPicsInfo>
                    </xml>
                    """
    })
    @DisplayName("用户从自定义菜单选择拍照发送出图片")
    public void testOnCustomMenuPicture(String rawXml) {
        webClient.post().uri("/test?signature=51ffed999d30aa1b9ee8682687deac357b244737&timestamp=1670420718&nonce=1946660312&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1670420901</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[pic_weixin]]></Event>
                    <EventKey><![CDATA[BLOOM_LIKE_SUMMER_FLOWER]]></EventKey>
                    <SendPicsInfo><Count>1</Count>
                    <PicList><item><PicMd5Sum><![CDATA[c4eb1ac953fe810e7436ca0421c92aa4]]></PicMd5Sum>
                    </item>
                    </PicList>
                    </SendPicsInfo>
                    </xml>
                    """
    })
    @DisplayName("用户从自定义菜单弹出图片或视频选择后发送")
    public void testOnCustomMenuAlbum(String rawXml) {
        webClient.post().uri("/test?signature=f30511a24fcfff39000e41c98c9a287744933f08&timestamp=1670420901&nonce=1822171742&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1670676030</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[location_select]]></Event>
                    <EventKey><![CDATA[FITTEST_SURVIVAL_LAST]]></EventKey>
                    <SendLocationInfo><Location_X><![CDATA[31.257009506225586]]></Location_X>
                    <Location_Y><![CDATA[121.59297180175781]]></Location_Y>
                    <Scale><![CDATA[15]]></Scale>
                    <Label><![CDATA[浦东新区平度路212号]]></Label>
                    <Poiname><![CDATA[浦东新区金桥公园(平度路东)]]></Poiname>
                    </SendLocationInfo>
                    </xml>
                    """
    })
    @DisplayName("用户从自定义菜单弹出的地图选择位置发送")
    public void testOnCustomMenuLocating(String rawXml) {
        webClient.post().uri("/test?signature=c87a38a6dbac347b4e817a39e4d4bb8d53791f93&timestamp=1670676030&nonce=1677940528&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xpath("/xml/Content").exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <xml><ToUserName><![CDATA[gh_de0f036ce08f]]></ToUserName>
                    <FromUserName><![CDATA[ooUo26seZPcU3qKfcMiXLneG3fO4]]></FromUserName>
                    <CreateTime>1668927673</CreateTime>
                    <MsgType><![CDATA[event]]></MsgType>
                    <Event><![CDATA[unsubscribe]]></Event>
                    <EventKey><![CDATA[]]></EventKey>
                    </xml>
                    """
    })
    @DisplayName("用户取消关注")
    public void testOnUnsubscribe(String rawXml) {
        webClient.post().uri("/test?signature=e3de61cbaaecd89a0b26f22df754a3d6331ebe69&timestamp=1668927673&nonce=417761816&openid=ooUo26seZPcU3qKfcMiXLneG3fO4")
                .header("User-Agent", "Mozilla/4.0")
                .header("Host", "127.0.0.1:20080")
                .header("Accept", "*/*")
                .header("Pragma", "no-cache")
                .header("X-Forwarded-For", "175.24.214.150")
                .contentType(MediaType.TEXT_XML)
                .contentLength(280)
                .bodyValue(rawXml)
                .exchange()
                .expectStatus().isOk();
    }
}
