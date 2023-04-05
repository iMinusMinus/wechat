package weixin.mp.facade;

import weixin.mp.facade.dto.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface WeixinUserApiFacade {

    // 用户管理
    /**
     * 创建标签
     */
    CompletableFuture<? extends UserLabel> labeling(String name);

    interface UserLabel {
        Integer id();
        String name();
        Integer count();
    }
    /**
     * 获取标签
     */
    CompletableFuture<? extends List<? extends UserLabel>> labeled();

    /**
     * 修改标签
     */
    CompletableFuture<Void> relabel(int tagId, String name);

    /**
     * 删除标签
     */
    CompletableFuture<Void> unlabeling(int tagId);

    /**
     * 获取标签下粉丝列表
     */
    CompletableFuture<? extends Pageable<String>> listLabeled(int tagId, String cursor);

    /**
     * 批量为用户打标
     */
    CompletableFuture<Void> mark(List<String> users, int tagId);

    /**
     * 批量取消标记
     */
    CompletableFuture<Void> unmark(List<String> users, int tagId);

    /**
     * 设置用户备注名
     */
    CompletableFuture<Void> remark(String openId, String comment);

    /**
     * 获取用户基本信息
     */
    CompletableFuture<? extends Customer> getCustomerInfo(WeixinClientUser clientUser);

    interface WeixinClientUser {
        String openId();
        String language();
        default boolean labelOnly() {
            return false;
        }
    }

    interface Customer {
        Boolean subscribed();
        String openId();
        String language();
        LocalDateTime subscriptionTime();
        String unionId();
        String remark();
        String groupId();
        List<Integer> tags();
        String subscribeScene();
        String scene();
        String sceneDescription();
    }

    /**
     * 批量获取用户基本信息
     */
    CompletableFuture<? extends List<? extends Customer>> listCustomers(WeixinClientUser[] clientUsers);

    /**
     * 获取用户列表
     */
    CompletableFuture<? extends Pageable<String>> listUsers(String fromOpenId);

    /**
     * 拉黑用户
     */
    CompletableFuture<Void> block(List<String> users);

    /**
     * 取消拉黑用户
     */
    CompletableFuture<Void> unblock(List<String> users);

    /**
     * 获取黑名单列表
     */
    CompletableFuture<? extends Pageable<String>> retrieveBlocked(String fromOpenId);
}
