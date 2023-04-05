package weixin.mp.facade;

import weixin.mp.domain.MenuItem;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 菜单管理
 * 注意：个性化菜单仅开放给已认证的公众号
 */
public interface WeixinMenuApiFacade {

    /**
     * 创建菜单/个性化菜单。自定义菜单为按照规则给不同用户不同菜单。当设置了多个个性化菜单时，优先匹配最近创建的个性化菜单
     * @param items 菜单信息
     * @param rule 菜单匹配规则，仅个性化菜单需要
     * @return 创建菜单结果，如果是创建个性化菜单，成功时会返回menuId
     */
    CompletableFuture<String> create(List<? extends MenuItem> items, MenuMatchRule rule);

    /**
     * 测试个性化菜单匹配结果
     * @param userId 关注着的openId或微信号
     * @return 个性化菜单项
     */
    CompletableFuture<? extends List<? extends MenuItem>> test(String userId);

    /**
     * 查询菜单/个性化菜单
     * @param custom true, 查询个性化菜单; false, 查询非个性化菜单
     * @return 查询个性化菜单才会同时返回menu和conditionalMenu
     */
    CompletableFuture<MenuBar> get(boolean custom);

    /**
     * 删除菜单/个性化菜单
     * @param menuId 仅删除自定义菜单时需传如menuId，不传则删除所有菜单
     * @return 删除结果
     */
    CompletableFuture<Void> delete(Long menuId);

    interface MenuBar {
        MenuResult fixedMenu();
        List<? extends CustomMenuResult> conditionalMenus();
    }

    interface MenuResult {

        /**
         *
         * @return 菜单项
         */
        List<? extends MenuItem> items();

        /**
         *
         * @return 菜单id，仅查询个性化菜单时，且个性化菜单存在，返回普通菜单id
         */
        Long menuId();

        /**
         *
         * @return 是否开启，仅非个性化菜单返回
         */
        boolean isEnabled();
    }

    interface CustomMenuResult {
        List<? extends MenuItem> items();
        MenuMatchRule rule();
        Long menuId();
    }

    interface MenuMatchRule {
        String tagId();
        String clientPlatform();
    }

}
