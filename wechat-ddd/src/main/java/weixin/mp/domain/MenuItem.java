package weixin.mp.domain;

import weixin.mp.facade.dto.Paper;

import java.util.List;

public interface MenuItem {
    /**
     * 菜单名称
     * @return 菜单名称
     */
    String name();

    /**
     * 菜单的类型
     * @return 菜单的类型
     */
    MenuType kind();

    /**
     * 含子菜单的菜单
     */
    interface BoxItem extends MenuItem {
        List<? extends MenuItem> items();
    }

    /**
     * 微信公众号管理后台配置的发送消息菜单
     */
    interface ValueItem extends MenuItem {
        /**
         * Text:保存文字到value；
         * Img、voice：保存 mediaID 到value；
         * Video：保存视频下载链接到value
         */
        String value();

        default String getValue() {
            return value();
        }
    }

    /**
     * API配置的菜单
     */
    interface KeyItem extends MenuItem {
        /**
         * click、scancode_push、scancode_waitmsg、pic_sysphoto、pic_photo_or_album、 pic_weixin、location_select：保存值到key
         * @return 推送事件的key
         */
        String key();
    }

    interface ViewItem extends MenuItem {
        /**
         *
         * @return 保存链接到url
         */
        String url();
    }

    interface ProgramletItem extends MenuItem {
        /**
         *
         * @return 网页链接
         */
        String url();

        /**
         *
         * @return 小程序的appid
         */
        String appId();

        /**
         *
         * @return 小程序的页面路径
         */
        String pagePath();
    }

    interface PaperItem extends MenuItem {
        List<? extends Paper> items();
    }
}
