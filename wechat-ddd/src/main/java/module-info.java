module wechat.ddd {

    requires java.base;
    requires transitive org.slf4j;
    requires java.xml;
    requires transitive jakarta.validation;

    exports weixin.mp.domain to wechat.spring, wechat.vanilla;
    exports weixin.mp.facade to wechat.spring, wechat.vanilla;
    exports weixin.mp.facade.dto to wechat.spring, wechat.vanilla;
    exports weixin.mp.application to wechat.spring, wechat.vanilla;
    exports weixin.pay.domain to wechat.spring, wechat.vanilla;
    exports weixin.work.domain to wechat.spring, wechat.vanilla;


}