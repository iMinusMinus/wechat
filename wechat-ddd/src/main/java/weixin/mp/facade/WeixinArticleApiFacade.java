package weixin.mp.facade;

import weixin.mp.facade.dto.ManualScript;
import weixin.mp.facade.dto.Pageable;
import weixin.mp.facade.dto.Paper;
import weixin.mp.facade.dto.Publication;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface WeixinArticleApiFacade {

    /**
     * 新建草稿
     * @param article 文章草稿
     * @return 返回mediaId
     */
    CompletableFuture<String> draft(List<? extends ManualScript> article);

    /**
     * 获取草稿
     * @param mediaId 草稿mediaId
     * @return 已保存的草稿
     */

    CompletableFuture<? extends List<? extends Paper>> retrieve(String mediaId);

    /**
     * 删除草稿
     * @param mediaId 草稿mediaId
     */
    CompletableFuture<Void> tear(String mediaId);

    /**
     * 修改草稿
     * @param mediaId 保存草稿的id
     * @param index 要更新的文章在图文消息中的位置
     * @param draft 已保存的草稿
     */
    CompletableFuture<Void> revise(String mediaId, int index, ManualScript draft);

    /**
     * 获取草稿总数
     * @return 草稿总数
     */
    CompletableFuture<Integer> count();

    /**
     * 获取草稿列表
     * @param offset 素材偏移位置，0开始
     * @param count 返回素材数量： 1~20
     * @param contentless 是否返回content
     * @return 草稿列表
     */
    CompletableFuture<? extends Pageable<? extends Publication>> list(int offset, int count, boolean contentless);

    /**
     * 发布。需先将草稿保存，然后进行发布
     * @param mediaId
     * @return 发布结果，成功仅代表提交发布成功，可能原创声名失败、平台审核不通过
     */
    CompletableFuture<? extends PublishResult> publish(String mediaId);

    interface PublishResult {
        String publishId();
        String msgDataId();
    }

    /**
     * 查询发布状态
     */
    CompletableFuture<? extends PublishStatusResult> status(String publishId);

    interface PublishStatusResult {
        String articleId();
        int status();
        List<? extends ArticleStatus> successArticles();
        int[] failedArticleIndexes();
    }

    interface ArticleStatus {
        int id();
        String url();
    }

    /**
     * 撤销发布
     */
    CompletableFuture<Void> cancel(String articleId, int index);

    /**
     * 获取已发布文章
     */
    CompletableFuture<? extends List<? extends Paper>> view(String articleId);

    /**
     * 获取成功发布列表
     */
    CompletableFuture<? extends Pageable<? extends Publication>> retrievePublished(int offset, int count, boolean contentless);

    /**
     * 打开已群发文章评论
     */

    /**
     * 关闭已群发文章评论
     */

    /**
     * 查看指定文章的评论数据
     */

    /**
     * 将评论标记精选
     */

    /**
     * 将评论取消精选
     */

    /**
     * 删除评论
     */

    /**
     * 回复评论
     */

    /**
     * 删除回复
     */
}
