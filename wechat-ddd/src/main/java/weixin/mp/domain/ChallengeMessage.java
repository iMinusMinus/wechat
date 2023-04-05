package weixin.mp.domain;

import java.util.Arrays;

/**
 * 服务器验证消息
 *
 * @author iMinusMinus
 * @date 2022-12-03
 */
public record ChallengeMessage(Context ctx, String signature, String timestamp, String nonce) {

    public boolean check() {
        String[] tmp = {timestamp, nonce, ctx.token()};
        Arrays.sort(tmp);
        String computedSignature = TrustableMessage.MP_INSTANCE.digest(String.join("", tmp));
        return signature.equalsIgnoreCase(computedSignature);
    }

}
