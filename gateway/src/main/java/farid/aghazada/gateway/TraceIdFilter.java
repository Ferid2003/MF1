package farid.aghazada.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import reactor.core.publisher.Mono;

@Component
public class TraceIdFilter implements WebFilter {

    static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = ServerRequestObservationContext.findCurrent(exchange.getAttributes())
                .map(context -> context.<TracingContext>get(TracingContext.class))
                .map(TracingContext::getSpan)
                .map(span -> span.context().traceId())
                .orElse(null);
        String path = exchange.getRequest().getPath().value();
        if (traceId == null || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", traceId)) {
            log.info("Routing {} {}", exchange.getRequest().getMethod(), path);
        }
        return chain.filter(exchange);
    }
}
