package farid.aghazada.resource_processor.Client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ResourceServiceClient {

    private final RestClient restClient;

    public ResourceServiceClient(@LoadBalanced RestClient.Builder builder, @Value("${resource.service.name}") String resourceServiceName) {
        this.restClient = builder.baseUrl("http://" + resourceServiceName).build();
    }

    public byte[] getSong(Integer id) {
        return restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/resources/{id}").build(id))
            .retrieve()
            .body(byte[].class);
    }
}
