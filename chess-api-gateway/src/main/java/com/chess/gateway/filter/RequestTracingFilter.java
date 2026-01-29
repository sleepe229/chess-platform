package com.chess.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class RequestTracingFilter implements WebFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);

        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        final String traceId = requestId;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(REQUEST_ID_HEADER, traceId)
                .build();

        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, traceId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(ctx -> ctx.put("traceId", traceId))
                .doOnSubscribe(subscription -> MDC.put("traceId", traceId))
                .doFinally(signalType -> MDC.remove("traceId"));
    }
}
