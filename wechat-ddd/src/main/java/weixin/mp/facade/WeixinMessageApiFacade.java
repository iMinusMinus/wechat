package weixin.mp.facade;

import weixin.mp.domain.MessageType;
import weixin.mp.facade.dto.Material;
import weixin.mp.facade.dto.MessageReceipt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface WeixinMessageApiFacade {

    // 模板消息接口
    /**
     * 添加模板
     */

    /**
     * 获取模板列表
     */
    CompletableFuture<? extends List<? extends MessageTemplate>> retrieveTemplates();

    interface MessageTemplate {
        String id();
        String title();
        String primaryIndustry();
        String secondaryIndustry();
        String content();
        String example();
    }

    /**
     * 删除模板
     */
    CompletableFuture<Void> removeMessageTemplate(String id);

    /**
     * 发送模板消息
     */
    CompletableFuture<Long> send(TemplateMessage msg);

    interface TemplateMessage {
        String receiver();
        String templateId();
        default String url() {
            return null;
        }
        default String appId() {
            return null;
        }
        default String pagePath() {
            return null;
        }
        default String scene() {
            return null;
        }
        default String title() {
            return null;
        }
        Map<String, ? extends PlaceHolder> data();
        String msgId();
    }

    interface PlaceHolder {
        String value();
        String color();
    }

    /**
     * 推送模板消息给到授权微信用户
     */
    CompletableFuture<Void> pushMessage(TemplateMessage content);

    // 群发消息
    /**
     * 根据标签/OpenId列表群发
     */
    CompletableFuture<? extends MessageReceipt> broadcast(String groupId, List<String> users, MessageType type, Material material, boolean forcePublish);

    interface ImageMaterial extends Material {
        String[] imageIds();
        String recommend();
        boolean enableComment();
        boolean onlyFansComment();
    }

    interface VideoMaterial extends Material {
        String title();
        String description();
    }

    /**
     * 删除群发
     */
    CompletableFuture<Void> cancelBroadcast(String msgId, Integer newsIndex, String url);

    /**
     * 发送给指定用户进行预览
     */
    CompletableFuture<Void> preview(String openId, String name, MessageType type, Material material);

    interface CardPreviewMaterial {
        String code();
        String openId();
        LocalDateTime timestamp();
        String signature();
    }

    /**
     * 查询群发消息状态
     */
    CompletableFuture<String> fetchBroadcastStatus(String msgId);

    /**
     * 设置群发速度
     */
    CompletableFuture<Void> changeBroadcastSpeed(int level);

    /**
     * 获取群发速度
     */
    CompletableFuture<? extends Transmission> retrieveBroadcastSpeed();

    interface Transmission {
        int level();
        int qps();
    }
}
