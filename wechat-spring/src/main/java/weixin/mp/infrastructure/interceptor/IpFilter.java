package weixin.mp.infrastructure.interceptor;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class IpFilter implements WebFilter {

    private static final Map<String, Function<String, String>> HEADER_PARSER = new HashMap<>();

    private static final Function<String, String> FORWARDED_PARSER =
            (value) -> Optional.ofNullable(value).flatMap(v -> Arrays.stream(v.split(";"))
                    .filter(pair -> "for".equals(pair.split("=")[0]))
                    .findFirst().map(kv -> kv.split("=")[1])).orElse(null);

    static {
        HEADER_PARSER.put("Forwarded", FORWARDED_PARSER); // standard
        HEADER_PARSER.put("X-Forwarded-For", Function.identity()); // nginx etc.
    }

    public IpFilter() {
        this(HEADER_PARSER.keySet());
    }

    public IpFilter(Collection<String> originalIpHeaders) {
        this.originalIpHeaders = originalIpHeaders;
    }

    private final Collection<String> originalIpHeaders;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 在配置时设置拦截URL格式
        String ip = null;
        InetSocketAddress requestIp = exchange.getRequest().getRemoteAddress();
        if (requestIp != null && !requestIp.getAddress().isAnyLocalAddress()
                && !requestIp.getAddress().isLoopbackAddress() && !requestIp.getAddress().isLinkLocalAddress()
                && !requestIp.getAddress().isSiteLocalAddress()) {
            ip = requestIp.getAddress().getHostAddress();
        }
        boolean flag = isLegalIp(ip);
        if (!flag) {
            for (String header : originalIpHeaders) {
                ip = exchange.getRequest().getHeaders().getFirst(header);
                flag = isLegalIp(ip);
                if (flag) {
                    break;
                }
            }
        }
        if (!flag) {
            // TODO 非微信IP
        }
        return chain.filter(exchange);
    }

    private boolean isLegalIp(String ip) {
        long legalIpCount = 0;
        List<InetAddress> weixinServerAddress = null; // TODO 请求微信服务器得到
        if (weixinServerAddress != null && !weixinServerAddress.isEmpty()) {
            legalIpCount = weixinServerAddress.stream().filter(addr -> addr.toString().equals(ip)).count();
        }
        return legalIpCount > 0;
    }
}
