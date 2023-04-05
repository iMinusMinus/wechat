package weixin.mp.facade;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

public interface WeixinApiFacade {


    /**
     * 生成带参数二维码:
     * <ul>
     *     <li>临时二维码。有过期时间的，最长可以设置为在二维码生成后的30天（即2592000秒）后过期，但能够生成较多数量</li>
     *     <li>永久二维码，是无过期时间的，但数量较少（目前为最多10万个）</li>
     * </ul>
     */
    CompletableFuture<? extends GeneratedQuickResponseResult> generateQuickResponseCode(Integer ttl, Integer sceneId, String sceneMark);

    interface GeneratedQuickResponseResult {
        String ticket();
        Integer ttl();
        String url();

    }

    /**
     * 通过ticket换取二维码（无需登录）
     * @param ticket
     * @return
     */
    CompletableFuture<InputStream> downloadQuickResponseCode(String ticket);

    /**
     * 短key
     */
    CompletableFuture<String> shorten(String original, int ttl);

    /**
     * 短key还原
     * @param shortUtl
     * @return
     */
    CompletableFuture<? extends OriginalResult> restore(String shortUtl);

    interface OriginalResult {
        String originalData();
        LocalDateTime createdAt();
        int ttl();
    }

    /**
     * 设置所属行业
     */
    CompletableFuture<Void> changeIndustry(String primaryIndustry, String secondaryIndustry);

    /**
     * 获取设置的行业信息
     */
    CompletableFuture<? extends CorpOperation> viewIndustry();

    interface CorpOperation {
        IndustryDescription main();
        IndustryDescription side();
    }

    interface IndustryDescription {
        String primaryIndustryDescription();
        String secondaryIndustryDescription();
    }

}
