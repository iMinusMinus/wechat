package weixin.mp.infrastructure.interceptor;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class AuthenticationFilter implements WebFilter {

    private static final String AK = "access-key";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String ak = exchange.getRequest().getHeaders().getFirst(AK);
        if (ak == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
        }
        // TODO
        return chain.filter(exchange);
    }
}
