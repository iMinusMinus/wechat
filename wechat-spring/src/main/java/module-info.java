module wechat.spring {
    // compile
    requires wechat.ddd;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.web;
    requires spring.webflux;
    requires reactor.core;
    requires org.reactivestreams;
    requires jakarta.validation;
    requires com.fasterxml.jackson.annotation;
    requires spring.data.redis;
    requires com.fasterxml.jackson.databind;

    // runtime
    requires spring.core;
    requires java.sql;

    // test
//    requires java.desktop;

    // Caused by: org.springframework.cglib.core.CodeGenerationException: java.lang.IllegalAccessException-->module wechat.spring does not open weixin to module spring.core
    // Caused by: java.lang.IllegalAccessException: class org.springframework.context.annotation.ConfigurationClassEnhancer$BeanFactoryAwareMethodInterceptor (in module spring.context) cannot access class weixin.Application$$SpringCGLIB$$0 (in module wechat.spring) because module wechat.spring does not export weixin to module spring.context
    opens weixin to spring.core, spring.beans, spring.context;

    exports weixin.mp.infrastructure.endpoint to spring.web;
    opens weixin.mp.infrastructure.rpc;
    opens weixin.mp.infrastructure.endpoint;
    opens weixin.mp.infrastructure.config;
    exports weixin.mp.infrastructure.config to spring.web;
    opens weixin.mp.infrastructure.exceptions;
    exports weixin.mp.infrastructure.endpoint.vo to spring.web;
    opens weixin.mp.infrastructure.endpoint.vo; // class org.springframework.beans.BeanUtils (in module spring.beans) cannot access class weixin.mp.infrastructure.endpoint.MessageController (in module wechat.spring) because module wechat.spring does not export weixin.mp to module spring.beans

    uses java.sql.Driver;
}