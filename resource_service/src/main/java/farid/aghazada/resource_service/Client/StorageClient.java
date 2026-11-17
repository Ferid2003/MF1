package farid.aghazada.resource_service.Client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import farid.aghazada.resource_service.DTO.StorageDTO;
import farid.aghazada.resource_service.Entity.ResourceState;
import farid.aghazada.resource_service.Exception.StorageException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StorageClient {

    private static final List<StorageDTO> STUB_STORAGES = List.of(
            new StorageDTO(1, "STAGING", "staging-bucket", "/files"),
            new StorageDTO(2, "PERMANENT", "permanent-bucket", "/files")
            );
    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public StorageClient(@LoadBalanced RestClient.Builder builder,
            @Value("${storage.service.name:storage-service}") String storageServiceName,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.restClient = builder.baseUrl("http://" + storageServiceName).build();
        this.circuitBreaker = circuitBreakerFactory.create("storageService");
    }

    public StorageDTO getStorage(ResourceState state) {
        List<StorageDTO> storages = circuitBreaker.run(() -> restClient.get()
                .uri("/storages")
                .retrieve()
                .body(new ParameterizedTypeReference<List<StorageDTO>>() {}),
                ex -> {
                    log.warn("Storage service unavailable ({}), using stub storages", ex.toString());
                    return STUB_STORAGES;
                });
        return storages.stream()
                .filter(s -> state.name().equals(s.storageType()))
                .findFirst()
                .orElseThrow(() -> new StorageException("No " + state + " storage configured"));
    }
}
