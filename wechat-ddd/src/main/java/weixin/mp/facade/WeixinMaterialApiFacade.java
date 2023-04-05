package weixin.mp.facade;

import weixin.mp.domain.MaterialType;
import weixin.mp.facade.dto.ManualScript;
import weixin.mp.facade.dto.Material;
import weixin.mp.facade.dto.Pageable;
import weixin.mp.facade.dto.Paper;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * <a href="https://developers.weixin.qq.com/doc/offiaccount/Asset_Management/New_temporary_materials.html">素材管理</a>
 * 临时素材保存3天，永久素材图文消息素材、图片素材上限为100000，其他类型为1000
 */
public interface WeixinMaterialApiFacade {

    interface TemporalMaterial extends Material {
        /**
         * @return 素材类型
         */
        MaterialType type();

        /**
         * @return 临时素材创建时间
         */
        LocalDateTime createdAt();
    }

    interface PermanentMaterial extends Material {
        /**
         * @return 仅新增图片素材返回
         */
        String url();
    }

    record VideoDescription(String title, String introduction) {}
    /**
     * 新增临时/永久素材
     * @param is 素材流
     * @param filename 素材文件名
     * @param forever 是否永久素材
     * @param description 视频素材描述
     */
    CompletableFuture<? extends Material> upload(InputStream is, String filename, boolean forever, VideoDescription description);

    /**
     * 新增图文永久素材/上传图文消息素材
     * @param news
     * @return mediaId
     */
    CompletableFuture<? extends NewsResult> addNews(List<? extends ManualScript> news, boolean broadcastPurpose);

    /**
     * 图文消息上传图片
     */
    CompletableFuture<String> uploadImage(InputStream is, String filename);

    interface NewsResult extends Material {
        MaterialType type();
        LocalDateTime createdAt();
    }

    /**
     * 获取临时/永久素材
     * @param mediaId 上传时返回的mediaId
     * @param forever 是否永久素材
     * @return 文件流，或视频、图文信息（包含下载url）
     */
    CompletableFuture<? extends Downloadable> download(String mediaId, boolean forever);

    interface Downloadable {}

    interface Stream extends Downloadable {
        String filename();
        InputStream body();
    }

    interface Video extends Downloadable {
        /**
         * 仅永久素材返回
         * @return
         */
        String title();

        /**
         * 仅永久素材饭hi
         * @return
         */
        String description();

        /**
         * 下载url
         * @return
         */
        String url();
    }

    /**
     * 仅永久素材有此类型
     */
    interface News extends Downloadable {
        List<? extends Paper> items();
    }

    /**
     * 删除永久素材
     */
    CompletableFuture<Void> recycle(String mediaId);

    /**
     * 获取素材总数
     */
    CompletableFuture<? extends MaterialDistribution> countMaterialByType();

    interface MaterialDistribution {
        int image();
        int voice();
        int video();
        int news();
    }

    /**
     * 获取素材列表
     * @param type 素材类型
     * @param offset 偏移位置
     * @param limit 分页大小
     * @return 分页结果，包含mediaId等信息
     */
    CompletableFuture<? extends Pageable<? extends Material>> list(MaterialType type, int offset, int limit);


}
