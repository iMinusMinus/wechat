package weixin.mp.domain;

import java.util.Arrays;

public record RequestMessageRetriever(Context ctx, String msgSignature, String timestamp, String nonce, String encryptMsg) {

    public boolean check()  {
        String[] tmp = new String[] {ctx.token(), timestamp, nonce, encryptMsg};
        Arrays.sort(tmp);
        String computedSignature = TrustableMessage.MP_INSTANCE.digest(String.join("", tmp));
        return msgSignature.equalsIgnoreCase(computedSignature);
    }

    public String retrieve(String encryptAlgorithm) {
        TrustableMessage.MessageWrapper wrapper = TrustableMessage.MP_INSTANCE.decrypt(ctx.key(), encryptMsg);
        if (wrapper.appId().equals(ctx.appId())) {
            return wrapper.content();
        }
        return null;
    }
}
