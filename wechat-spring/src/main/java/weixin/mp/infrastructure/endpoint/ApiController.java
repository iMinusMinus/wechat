package weixin.mp.infrastructure.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    @GetMapping(value = ExposedPath.JS_API, produces = {MediaType.TEXT_PLAIN_VALUE})
    @ResponseBody
    public String challenge(@PathVariable("id") String id, @PathVariable("nonce") String nonce) {
        log.info("JS接口安全域名验证，id={}, nonce={}", id, nonce);
        return nonce;
    }
}
