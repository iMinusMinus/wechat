package weixin.mp.domain;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public record ResponseMessageBuilder(Context ctx, long createTime, String nonce, ReplyMessage reply) {

    private static final String XML_FORMAT = """
            <xml>
            <Encrypt><![CDATA[%1$s]]></Encrypt>
            <MsgSignature><![CDATA[%2$s]]></MsgSignature>
            <TimeStamp>%3$s</TimeStamp>
            <Nonce><![CDATA[%4$s]]></Nonce>
            </xml>
            """;

    public String build() {
        // 确认接收，无需加密
        if (reply == ReplyMessage.ACK || reply == ReplyMessage.NO_RETRY) {
            return reply.toXml();
        }
        // 未开启加密或混淆模式，无需加密
        if (ctx.strict() == null || !ctx.strict()) {
            return reply.toXml();
        }
        // 开启加密模式
        String timestamp = String.valueOf(createTime / 1000);
        byte[] random = genRandom();
        byte[] data = reply.toXml().getBytes(StandardCharsets.UTF_8);
        byte[] networkBytesOrder = new byte[4];
        int len = data.length;
        for(int i = 3; i >= 0; i--) {
            networkBytesOrder[i] = (byte) (len & 0xFF);
            len >>= 8;
        }
        byte[] appId = ctx.appId().getBytes(StandardCharsets.UTF_8);

        byte[] plaintext = new byte[random.length + networkBytesOrder.length + data.length + appId.length];
        System.arraycopy(random, 0, plaintext, 0, random.length);
        System.arraycopy(networkBytesOrder, 0, plaintext, random.length, networkBytesOrder.length);
        System.arraycopy(data, 0, plaintext, random.length + networkBytesOrder.length, data.length);
        System.arraycopy(appId, 0, plaintext, random.length + networkBytesOrder.length + data.length, appId.length);
        String ciphertext =  TrustableMessage.MP_INSTANCE.encrypt(ctx.key(), plaintext);

        String[] tmp = {ctx.token(), timestamp, nonce, ciphertext};
        Arrays.sort(tmp);
        String signature = TrustableMessage.MP_INSTANCE.digest(String.join("", tmp));
        return XML_FORMAT.formatted(ciphertext, signature, timestamp, nonce);
    }

    private byte[] genRandom() {
        SecureRandom sr = new SecureRandom();
        byte[] random = new byte[16];
        int i = 0;
        for (;;) {
            int r = sr.nextInt(122 - 48) + 48; // 0=>48, z=>122
            if((r >= '0' && r <= '9') || (r >= 'A' && r <= 'Z') || (r >= 'a' && r <= 'z')) {
                random[i++] = (byte) r;
            }
            if (i >= 16) {
                break;
            }
        }
        return random;
    }

}
