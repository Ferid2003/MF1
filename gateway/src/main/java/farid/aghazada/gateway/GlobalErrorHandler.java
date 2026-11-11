package farid.aghazada.gateway;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Map;

import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(-2)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private final JsonMapper jsonMapper;

    public GlobalErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatusCode status = resolveStatus(ex);
        Map<String, String> body = Map.of(
                "errorMessage", resolveMessage(status, exchange.getRequest().getPath().value()),
                "errorCode", String.valueOf(status.value()));

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.fromSupplier(
                () -> response.bufferFactory().wrap(jsonMapper.writeValueAsBytes(body))));
    }

    private HttpStatusCode resolveStatus(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof ConnectException || t instanceof UnknownHostException) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
        }
        if (ex instanceof ResponseStatusException rse) {
            return rse.getStatusCode();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(HttpStatusCode status, String path) {
        return switch (status.value()) {
            case 404 -> "Route '" + path + "' not found";
            case 503 -> "Service is temporarily unavailable. Please try again later.";
            case 504 -> "Service did not respond in time. Please try again later.";
            default -> status.is5xxServerError()
                    ? "An error occurred on the server"
                    : reasonPhrase(status);
        };
    }

    private String reasonPhrase(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known != null ? known.getReasonPhrase() : "Request failed";
    }
}
