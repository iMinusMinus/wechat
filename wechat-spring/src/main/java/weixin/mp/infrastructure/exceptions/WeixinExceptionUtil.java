package weixin.mp.infrastructure.exceptions;

import org.springframework.http.HttpStatus;

public abstract class WeixinExceptionUtil {

    // TODO
    public static RuntimeException create(int code, String message) {
        switch (code) {
            case -1: // 系统繁忙，此时请开发者稍候再试
                return new RetryableException();
            case 45011: // API 调用太频繁，请稍候再试
            case 45066: // 相同 clientmsgid 重试速度过快，请间隔1分钟重试
            case 53501: // 频繁请求发布
                return new ClientError(code, message, HttpStatus.TOO_MANY_REQUESTS);
            case 47001: // 解析 JSON/XML 内容错误
            case 61450: // 系统错误
                return new ServerError(code, message, HttpStatus.INTERNAL_SERVER_ERROR);
            case 9001002: // 远端服务不可用
                return new ServerError(code, message, HttpStatus.SERVICE_UNAVAILABLE);
            case 48001: // api 功能未授权
            case 48004: // api 接口被封禁
            case 48005: // api 禁止删除被自动回复和自定义菜单引用的素材
            case 48006: // api 禁止清零调用次数，因为清零次数达到上限
            case 48008: // 没有该类型消息的发送权限
            case 53500: // 发布功能被封禁
                return new ClientError(code, message, HttpStatus.FORBIDDEN);
            default:
                return new  ClientError(code, message, HttpStatus.BAD_REQUEST);
        }
    }
}
