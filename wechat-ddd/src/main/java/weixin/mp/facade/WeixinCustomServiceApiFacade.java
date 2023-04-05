package weixin.mp.facade;

import weixin.mp.domain.ReplyMessage;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 微信客服
 * <ul>
 *     <li><a href="https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Service_Center_messages.html">客服账号管理、发送消息及输入状态</a></li>
 *     <li><a href="https://developers.weixin.qq.com/doc/offiaccount/Customer_Service/Forwarding_of_messages_to_service_center.html">消息转发到客服</a></li>
 *     <li><a href="https://developers.weixin.qq.com/doc/offiaccount/Customer_Service/Session_control.html">会话管理</a></li>
 *     <li><a href="https://developers.weixin.qq.com/doc/offiaccount/Customer_Service/Obtain_chat_transcript.html">获取聊天记录</a></li>
 * </ul>
 * @author iMinusMinus
 * @date 2023-03-04
 */
public interface WeixinCustomServiceApiFacade {

    /**
     * 添加客服帐号，每个公众号最多添加100个客服账号
     * @param account 账号，格式必须是"账号@公众号"
     * @param nickname 昵称
     * @param password 密码，可为空，值为md5摘要
     * @return 添加结果
     */
    CompletableFuture<Void> addAccount(String account, String nickname, String password);

    /**
     * 发送绑定客服账号邀请
     * @param account 完整客服帐号
     * @param openId 接收绑定邀请的客服微信号
     * @return
     */
    CompletableFuture<Void> bindAccountInviting(String account, String openId);

    /**
     * 修改客服帐号
     * @param account 账号
     * @param nickname 昵称
     * @param password 密码
     * @return 修改结果
     */
    CompletableFuture<Void> updateAccount(String account, String nickname, String password);

    /**
     * 删除客服帐号
     * @param account 账号
     * @return 删除结果
     */
    CompletableFuture<Void> removeAccount(String account);

    /**
     * 设置客服帐号的头像
     * @param is 头像图片文件必须是jpg格式，推荐使用640*640大小的图片以达到最佳效果
     * @param filename 图像名称
     * @param account 客服账号
     * @return
     */
    CompletableFuture<Void> changeAvatar(InputStream is, String filename, String account);

    /**
     * 获取所有客服账号
     * @return 客服清单，每个条目包含客服id、账号、昵称、头像
     */
    CompletableFuture<List<Account>> listAccounts();

    /**
     * @param id 客服工号
     * @param account 完整客服账号，格式为：账号前缀@公众号微信号
     * @param nickname 客服昵称
     * @param avatar 头像url
     */
    record Account(String id, String account, String nickname, String avatar) {}

    /**
     * 发消息
     * @param replyMessage 消息内容
     * @return 发送结果
     */
    CompletableFuture<Void> echo(ReplyMessage replyMessage);

    /**
     * 返回客服当前输入状态给用户
     * @param toOpenId 用户
     * @param command 输入状态
     * @return
     */
    CompletableFuture<Void> displayStatus(String toOpenId, Command command);

    enum Command {
        TYPING("Typing"),
        CANCEL_TYPING("CancelTyping");

        private String value;
        private Command(String value) {
            this.value =value;
        }

        public String getValue() {
            return value;
        }
    }

}
